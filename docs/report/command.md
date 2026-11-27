# Command (Behavioral)

## The problem

In the original code, a move changes the board and keeps no record of what it changed. `GamePanel.moveChessman()` (`GamePanel.java`, lines 281–313 at commit `7979bde`) overwrites five things:

- the captured piece, when it writes the moving piece onto the target square (line 298);
- the pawn, when promotion replaces it with a new Queen (line 294);
- the pawn's `startPosition` flag, which allows its two-square first move (line 290);
- the piece's `notMoved` flag, which decides whether a king or rook can still castle (line 301);
- the rook's square and its `notMoved` flag, when `castling()` moves it (line 288).

The only memory of a move is `_lastMove`, which holds two squares so the board can highlight them. That is not enough to reverse the move. So the game cannot offer Undo. A move is also not something the program can hold: it is a method call that runs and is gone, so nothing can store it, list it or replay it.

We cannot recompute the lost values after the fact. For example, a king that moves away and back stands on its starting square again, but it has lost the right to castle. The only way to undo correctly is to save each old value before overwriting it.

## The solution

We applied the Command pattern, which turns an action into an object. A new package, `chess.moves`, holds four types:

- **`Command`** (interface): `execute()` and `undo()`.
- **`MoveCommand`**: one normal move, capture or promotion. In `execute()` it first saves what it is about to overwrite (the captured piece, the old `notMoved` and `startPosition` flags), then makes the move. `undo()` puts the saved values back. On promotion the pawn object never changes, so undo simply puts the same pawn back.
- **`CastlingCommand`**: castling is two moves, so this class holds two `MoveCommand`s (king and rook). `execute()` runs them in order, and `undo()` runs them in reverse. This is a *macro command*: a command built from other commands.
- **`MoveHistory`**: two stacks of `Command`s. `execute(c)` runs a command, pushes it on the undo stack and clears the redo stack. `undo()` moves the top command to the redo stack, and `redo()` moves it back. Both do nothing when their stack is empty.

`GamePanel.moveChessman()` now calls `history.execute(createMove(from, to))`. The new `createMove()` holds the rules the old code had inline: castling, promotion, or a plain move. Each game mode overrides `undo()` and `redo()` with its own turn rule. Hot-seat undoes one move and gives the turn back. Against the AI, it undoes two moves (yours and the AI's reply), and only while the AI is not thinking. LAN shows a message, because undoing on one side would make the two players' boards differ. A new Edit menu in `GameFrame` calls `undo()` and `redo()` (Cmd/Ctrl+Z and Cmd/Ctrl+Shift+Z).

| Command role | Class in this project |
|---|---|
| Command | `Command` |
| Concrete Command | `MoveCommand`, `CastlingCommand` |
| Receiver (the objects that change) | the board `piecesBoard` and its `Chessman` objects |
| Invoker (runs commands, keeps history) | `MoveHistory` |
| Client (creates the command) | `GamePanel.createMove()` |

## Class diagrams

**Before** (`docs/diagrams/command-before.png`): `GamePanel` changes the board and the pieces directly inside `moveChessman()`. There is no class for a move and no history, only `_lastMove` with two squares.

**After** (`docs/diagrams/command-after.png`): `GamePanel` creates commands and hands them to `MoveHistory`. `MoveCommand` and `CastlingCommand` implement `Command`, and `CastlingCommand` is made of two `MoveCommand`s. `MoveHistory` depends only on the `Command` interface, so it does not know whether a command is a plain move or castling. The three game modes override `undo()` and `redo()`.

## Why Command

Command fits because the problem is that an action disappears after it runs. Once a move is an object, it can be stored on a stack, reversed with `undo()` and replayed with `execute()`, and that is all Undo and Redo need. It also keeps one job per class. `MoveCommand` knows how to change and restore the board. `MoveHistory` knows the order of moves. The game modes know whose turn it is. For example, the turn rule is not inside `MoveCommand`, because hot-seat, AI and LAN games each treat turns differently.

The alternative we considered is **Memento**, which saves a snapshot of the whole board before each move and restores it on Undo. Memento needs no undo code. But each snapshot must copy all 64 squares and every piece deeply, or later moves would change the saved copy. A chess move changes very little (two squares and a few flags), so saving only those values is smaller and simpler. A move object is also useful beyond Undo: it could feed a move list, a replay, or the LAN connection.

We also give credit to the original code. `GamePanel.opponentTurn()` is already a Template Method: the parent class fixes the flow of a turn, and each game mode fills in its own step. Our per-mode `undo()` and `redo()` follow the same idea. We do not claim that pattern as ours.

## Trade-offs and future work

The main cost is that every rule that changes the board now needs two pieces of code: `execute()` and an exact reverse in `undo()`. If a developer later adds *en passant* (a pawn capture that removes a piece from a square other than the target) and forgets to save that pawn, Undo will silently restore the wrong board. Memento would not have this risk. The change also adds four files and about 260 lines (274 added, 17 removed). The history also lives in memory only, so it is lost when the game closes.

While writing this pattern we found the same smell in a second place. `AI.minMax`, `min` and `max` "undo" their trial moves by moving the piece back (`AI.java`, lines 28 and 31 at `7979bde`, and the matching lines in `min()` and `max()`). That does not restore a captured piece or the flags. We left it alone, because `AI.minMax` is the site of our Prototype pattern and changing it would change how the AI plays. A next step is to let the AI use `MoveCommand` for its trial moves too. Another is to move the per-mode undo rules into the opponent classes once our Strategy pattern replaces the three `GamePanel` subclasses.
