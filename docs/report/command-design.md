# Stage 3, Behavioral: Command for moves (design)

Working notes for the Command pattern: the smell, why Command, and the classes to write.
Diagram: `docs/diagrams/command-before.drawio`. The "after" diagram comes once the code works.

## 1. Smell

`GamePanel.moveChessman()` (`GamePanel.java`, lines 281-313) changes the game and keeps no record of the change:

| Line | What it overwrites | What is lost |
|---|---|---|
| 298 | `piecesBoard[new] = piece` | the captured piece |
| 294 | `piece = factory.getPiece(QUEEN, ...)` | the pawn that was promoted |
| 290 | `pawn.startPosition = false` | whether the pawn could still move two squares |
| 301 | `piece.notMoved = false` | whether the king or rook could still castle |
| 288 | `castling(...)` moves the rook | the rook's old square and its `notMoved` |

`_lastMove` (line 25) keeps only the two squares, and only to draw the highlight.
So the game can't undo a move.

The move is also not a thing you can hold. It is a method call that runs and is gone.
Nothing can store it, list it, or reverse it.

**Extra finding, for the oral.** The AI tries to undo its pretend moves in `AI.minMax`
(`AI.java`, lines 28 and 31, and again in `min()` and `max()`) by calling `moveChessmanAI(p.start, p.end, ...)`. That moves the piece
back but doesn't bring the captured piece back, and the flags stay `false`. It's the same problem in
a second place. We leave it alone: `AI.minMax` is Mohammed's Prototype site, and fixing it would
change how the AI plays.

## 2. Why Command

Command turns an action into an object, so it can be stored, undone and redone.

A move becomes a `MoveCommand` object. The object saves what it is about to overwrite, so it can
put everything back. A list of these objects is the game's history, which gives us Undo and Redo.

**The alternative: Memento.** Memento saves a snapshot of the whole board before each move.
Undo then puts the snapshot back.

| | Command | Memento |
|---|---|---|
| What it saves per move | only what changes (a piece, 2 squares, 2 flags) | all 64 squares and every piece's fields |
| Undo code | each command must reverse its own changes | none: restore the snapshot |
| Risk | forget one field, undo is wrong | copies must be deep (new piece objects), or the snapshot changes too |
| Redo | run `execute()` again | needs a second list of snapshots |

We chose Command because a chess move changes very little, and because a move as an object is
useful beyond undo (a move list, a replay, sending a move over LAN).

**Trade-off.** Every rule that changes the board needs both `execute()` and `undo()` code.
If someone later adds en passant and forgets to save the captured pawn, undo silently breaks.

**Credit to the original code.** `GamePanel.opponentTurn()` is already a Template Method: the parent
fixes the flow, each mode fills in its own turn. We don't claim it; we use the same idea for undo.

## 3. Roles

| Command role (GoF) | Class here | Job |
|---|---|---|
| Command | `Command` (interface) | `execute()` and `undo()` |
| ConcreteCommand | `MoveCommand`, `CastlingCommand` | do one move; save what they overwrite; reverse it |
| Receiver | `Chessman[][]` board and the pieces | the objects that actually change |
| Invoker | `MoveHistory` | runs commands and keeps the undo and redo stacks |
| Client | `GamePanel` | decides which command a click means, and creates it |

New package: `chess.moves` (folder `src/chess/moves/`).

## 4. Classes

### `Command` (interface)

```
+ execute(): void
+ undo(): void
```

### `MoveCommand implements Command`

One move: a normal move, a capture, or a promotion.

Fields:
```
- board: Chessman[][]
- piece: Chessman          the piece that moves
- from: Position
- to: Position
- promotion: Chessman      the new Queen, or null when there's no promotion
- captured: Chessman       saved in execute(), may be null
- oldNotMoved: boolean     saved in execute()
- oldStartPosition: boolean  saved in execute(), only used when piece is a Pawn
```

Constructor: `MoveCommand(board, piece, from, to, promotion)`.

`execute()`, in this order:
1. Save: `captured` = what's on `to`; `oldNotMoved` = `piece.notMoved`; if the piece is a `Pawn`, `oldStartPosition` = its `startPosition`.
2. If it's a `Pawn`, set `startPosition` to `false`.
3. The piece that lands is `promotion` if there is one, else `piece`.
4. Put it on `to`, empty `from`, set its `pos` to `to` and its `notMoved` to `false`.

`undo()`: the same steps backwards.
1. Put `piece` back on `from` and set its `pos` to `from`.
2. Put `captured` back on `to` (it's `null` for a plain move, which empties the square).
3. Restore `piece.notMoved` from `oldNotMoved`, and the pawn's `startPosition` from `oldStartPosition`.

The captured piece never changed its own `pos`, so it needs nothing else.
On promotion the pawn's `pos` never changed either, so undo puts back the same pawn object.

### `CastlingCommand implements Command`

Castling is two moves: the king's and the rook's. This class holds two `MoveCommand`s and reuses them.

```
- kingMove: MoveCommand
- rookMove: MoveCommand
+ CastlingCommand(kingMove: MoveCommand, rookMove: MoveCommand)
+ execute(): void   kingMove, then rookMove
+ undo(): void      rookMove, then kingMove (reverse order)
```

### `MoveHistory` (the Invoker)

Two stacks: `ArrayDeque<Command>` with `push` and `pop`.

```
- undoStack: Deque<Command>
- redoStack: Deque<Command>
+ execute(c: Command): void   c.execute(); push c on undoStack; clear redoStack
+ undo(): void                pop from undoStack; c.undo(); push on redoStack
+ redo(): void                pop from redoStack; c.execute(); push on undoStack
+ canUndo(): boolean
+ canRedo(): boolean
+ undoCount(): int
+ redoCount(): int
```

A new move clears the redo stack, as in every editor.

### `GamePanel` (the Client), changes

- New field: `protected final MoveHistory history = new MoveHistory();`
- New `private Command createMove(Position from, Position to)`. It holds the rules the old method had:
  - The king hasn't moved and `to.x` is 2 or 6 → castling. The rook goes from column 0 to 3 (when `to.x` is 2) or from column 7 to 5 (when `to.x` is 6). Return a `CastlingCommand`.
  - A pawn reaching row 0 or 7 → make the Queen with `factory.getPiece(...)` and pass it as `promotion`.
  - Anything else → a `MoveCommand` with `promotion = null`.
- `moveChessman(newPosition, oldPosition)`: keep the `_lastMove` lines and the check, checkmate and stalemate code.
  Replace lines 285-301 with `history.execute(createMove(oldPosition, newPosition))`.
  The piece's colour for the check code: read it from `piecesBoard[newPosition.x][newPosition.y]` after the move.
- New `protected void refreshAfterUndo()`: clear `_selected`, `_possibleMoves` and `_lastMove`; set
  `_checkPosition` to the king in check (try white, then black) or `null`; `repaint()`.
- New `public void undo()` and `public void redo()`, empty in `GamePanel`. Each mode overrides them.
- Keep `static castling(...)`: `AI.moveChessmanAI` still uses it.

### Turn rules per mode (overrides)

Each mode already decides its own turn in `opponentTurn()`. Undo follows the same split.

| Mode | `undo()` | `redo()` |
|---|---|---|
| `GamePanelHot` | if `history.canUndo()`: undo 1 move, flip `whiteMove`, `refreshAfterUndo()` | the same with redo |
| `GamePanelAI` | only if `enabled` (the AI isn't thinking) and `history.undoCount() >= 2`: undo 2 moves (the AI's reply and yours), `refreshAfterUndo()` | the same with `redoCount() >= 2` |
| `GamePanelLAN` | show a message: undo isn't available in LAN games | the same |

Why pairs against the AI: undoing only your move would leave the AI's reply on the board, and it
would be the AI's turn with no thread running. `whiteMove` never changes in AI mode, so it needs no flip.

Why not LAN: the other player's board would no longer match yours.

Coordinate with Rayan: his Strategy (Stage 3) moves `opponentTurn()` into opponent classes.
These undo rules belong with the opponent too, so agree with him where they live.

### `GameFrame`, changes

`GameFrame` already sets an empty `JMenuBar`. Add an "Edit" menu with:
- "Undo": shortcut Cmd/Ctrl+Z, calls `gamePanel.undo()`
- "Redo": shortcut Cmd/Ctrl+Shift+Z, calls `gamePanel.redo()`

Shortcut key: `KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx())`.

## 5. Coding order

One file at a time. Compile after each one.

1. Branch `stage3-command` from `master`.
2. `Command.java`
3. `MoveCommand.java`
4. `CastlingCommand.java`
5. `MoveHistory.java`
6. `GamePanel.java`: field, `createMove`, new `moveChessman`, `refreshAfterUndo`, empty `undo`/`redo`
7. `GamePanelHot`, `GamePanelAI`, `GamePanelLAN` overrides
8. `GameFrame.java` menu

## 6. Run checks

- [x] A whole game without undo plays exactly as before (hot-seat and vs AI)
- [x] Plain move → undo → piece back, turn back to the same player
- [x] Capture → undo → the captured piece is back on its square
- [x] Pawn's first move → undo → the pawn can still move two squares
- [x] Promotion → undo → a pawn again, not a Queen
- [x] Castling → undo → king and rook back, and castling is still offered
- [x] Undo then redo → the same position again
- [x] Undo, then a new move → redo does nothing
- [x] vs AI: undo takes back your move and the AI's reply; it does nothing while the AI is thinking
- [x] LAN: undo shows the message and changes nothing
- [x] Undo with an empty history does nothing and doesn't crash
