# Strategy (Behavioral)

## The problem

The game has three modes: hot-seat (two people on one computer), against the AI, and LAN (against a player on another computer). The original code handles them with the Template Method pattern. `GamePanel` is abstract and declares one abstract step, `opponentTurn()` (`GamePanel.java`, line 40). Its `checkChessmanMove()` (lines 234–250) is the template: it plays the local player's move with `moveChessman(...)` and then calls `opponentTurn()` (lines 246–247). Each mode is a subclass that fills in that step. `GamePanelHot` swaps the colour to move, `GamePanelAI` runs `AI.minMax` on a background thread, and `GamePanelLAN` locks the board until the other player answers. `Menu.java` (lines 50–79) decides the mode by choosing which subclass to create.

This works, but the opponent is not one step. Much of each opponent lives outside `opponentTurn()`:

- **Set-up runs in the constructors.** `GamePanelAI` asks which colour you want (`chooseColor()`, called on line 18), and `GamePanelLAN` asks for the address and connects (`getSocketIP()`, line 26).
- **LAN changes the game's own move method.** `GamePanelLAN` overrides `moveChessman` (lines 100–104) so that every move is sent over the network. A move that arrives from the other player must then call `super.moveChessman` (line 83) to avoid being sent back. Any change to how moves are played, such as the Command pattern added in this stage, has to keep working with that override.
- **The opponent is fixed by the class.** The kind of opponent is decided by inheritance when the panel is created. Adding a new kind of opponent means adding another subclass of the whole game screen.

## The solution

We applied the Strategy pattern. The opponent is now an object that the game screen holds, not a subclass of it.

- **Strategy: `OpponentStrategy`** (new package `chess.opponents`). An interface with three methods: `start(game)` for set-up before the first move, `opponentTurn(from, to)` when the local player has moved, and `stop()` when the game closes.
- **Concrete strategies:**
  - `HumanOpponent` (hot-seat) swaps the colour to move.
  - `AIOpponent` asks for a colour in `start` and plays `AI.minMax` on a background thread on its turn.
  - `NetworkOpponent` connects in `start`, sends the local move on its turn, and closes the connection in `stop`. It reaches the network only through `OpponentConnection`, the Stage 2 Adapter, which it uses unchanged.
- **Context: `GamePanel`.** No longer abstract. It takes an `OpponentStrategy` in its constructor, calls `start` once the board is ready, calls `opponentTurn(oldPosition, newPosition)` after the local player's move (lines 260–265), and calls `stop` in `closeFrame()`.
- **Client: `Menu`.** Each button now creates the same `GamePanel` with a different strategy (lines 51–81), for example `new GamePanel(new AIOpponent())`.

`GamePanelHot`, `GamePanelAI` and `GamePanelLAN` were deleted. Their code moved into the three strategies with the same behaviour. The `super.moveChessman` trick is no longer needed. Only the local player's click reaches `opponentTurn`, while AI and network moves call `moveChessman` directly, so a received move can never be sent back. We agreed this point with the Command pattern in the same stage: `moveChessman` creates and runs the move command, and the strategy is called right after it, for local moves only.

## Class diagrams

**Before** (`docs/diagrams/strategy-before.drawio`): `GamePanel` is abstract, with `GamePanelHot`, `GamePanelAI` and `GamePanelLAN` extending it. `Menu` creates one of the subclasses. Notes mark `checkChessmanMove` as the template and list the opponent code that sits outside `opponentTurn()`.

**After** (`docs/diagrams/strategy-after.drawio`): one `GamePanel` holds an `OpponentStrategy`. `HumanOpponent`, `AIOpponent` and `NetworkOpponent` implement the interface. `NetworkOpponent` also implements `MoveListener` and holds an `OpponentConnection` from Stage 2. Notes name the roles and show the two lines in `checkChessmanMove` where the turn passes to the opponent.

## Why Strategy

Template Method and Strategy solve similar problems in different ways. Template Method varies one step through inheritance, fixed when the object is created. Strategy varies a whole behaviour through composition, as an object passed to the context. The opponent here is a whole behaviour: set-up, its turn, and clean-up. So it fits Strategy better than a single overridden step. With Strategy, the game screen is one class, the opponent code for each mode sits in one place, and a new opponent (for example a stronger AI) is a new class that implements `OpponentStrategy`, with no change to `GamePanel`. Removing the `moveChessman` override also leaves that method to the game and its Command pattern alone.

We considered three alternatives. **Keeping Template Method** would be fine if only `opponentTurn()` varied, but set-up and move-sending would still be spread across constructors and overrides. **A `switch` on a mode value inside `GamePanel`** would remove the subclasses but put mode checks in several methods of one large class. **State** looks similar in structure, but State is for behaviour that changes while the object runs. The opponent stays the same for a whole game, which is what Strategy describes.

This stage applies to a different part of the program from our Stage 2 Adapter. The Adapter deals with network transport, meaning how bytes travel between two computers, and is only used in LAN mode. Strategy deals with who the opponent is and how it chooses its moves, in all three modes. `NetworkOpponent` uses the Adapter, but it does not change it.

## Trade-offs and future work

The strategies hold a reference back to `GamePanel` and change its public fields (`whiteMove`, `enabled`, `piecesBoard`), so the context and the strategies still depend on each other. A narrower interface on `GamePanel` would reduce this. The code is not shorter: the three subclasses had 220 lines and the four new types have 276, mostly because of the interface and comments. As in the original, set-up runs from the `GamePanel` constructor, so `start` receives a panel that is not yet inside a window. One small change is deliberate. Cancelling the LAN connection dialog now throws a `CancellationException`, which the menu catches, so no game window opens. The original reached the same result by accident, through a `NullPointerException`.

Normal play is unchanged. We tested it with a script that opens the real game windows, clicks the board and answers the dialogs, and ran the same 28 checks on the code before and after the change. Hot-seat covered turn switching and Fool's Mate ending the game. AI covered playing as White and as Black, with the AI answering each move. LAN covered two copies connecting over 127.0.0.1, moves in both directions, blocked out-of-turn moves, the "Connection is lost" message, a wrong port, and cancelling. All 28 passed on both versions.

Undo (the Command pattern, also Stage 3) is different in each mode: one move in hot-seat, two against the AI (the AI's move and the player's), and none over LAN. When the two branches are merged, this rule will move from the old subclasses into the matching strategies.
