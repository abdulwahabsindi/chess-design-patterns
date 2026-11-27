# Adapter (Structural)

## The problem

LAN mode lets two copies of the game play each other over the network, and all of its networking is written inside the game screen. `GamePanelLAN` holds four networking fields: `Socket socket`, `ObjectOutputStream oos`, `ObjectInputStream ois` and `ServerSocket serverSocket` (`GamePanelLAN.java`, lines 19–22). Six of its ten methods work directly on them. `connect()` and `createServer()` (lines 63–120) first try to join a game at the address and, if nobody is there, host one and wait. `sendData()` (lines 122–133) sends a move as two `writeObject` calls. `createReciver()` (lines 79–104) starts a thread that calls `readObject()` twice for every move, and `resetSockets()` (lines 152–171) closes everything. So the class that draws the board and runs the turns also knows how Java sockets and object streams work. Changing the transport, for example to a WebSocket or a fake connection for testing, means rewriting the game screen, and the connection cannot be tested without opening the GUI.

## The solution

We applied the Adapter pattern (object adapter) and moved every line of socket code into a new package, `chess.network`:

- **Target: `OpponentConnection`.** The interface the game wants: `sendMove(from, to)`, `startListening(listener)`, `isHost()` and `close()`. It talks about moves, not sockets.
- **Adapter: `SocketConnectionAdapter`.** It implements `OpponentConnection` and holds the four Java objects. `sendMove(from, to)` becomes two `writeObject` calls, and the receiver thread turns each pair of `readObject()` calls into one `onMoveReceived(from, to)` call. The join-or-host rule is kept in one static method, `connectOrHost(address, port)`.
- **Adaptees: `Socket`, `ServerSocket`, `ObjectOutputStream`, `ObjectInputStream`.** Java's own classes, unchanged.
- **Client: `GamePanelLAN`.** It keeps one field, `OpponentConnection connection`, and implements a small callback interface, `MoveListener` (`onMoveReceived`, `onConnectionLost`), so the adapter can report incoming moves without knowing about the game or Swing.

`GamePanelLAN` went from 179 to 118 lines and no longer imports `Socket`, `ServerSocket` or the object streams. The only networking types left are `InetAddress` and `UnknownHostException`, which the connection dialog uses to read the address. The adapter also fixes the argument order: the game calls `moveChessman(new, old)`, while the connection speaks in `(from, to)`.

## Class diagrams

**Before** (`docs/diagrams/adapter-before.drawio`): `GamePanelLAN` extends `GamePanel` and points straight at `Socket`, `ServerSocket`, `ObjectOutputStream` and `ObjectInputStream`. The notes show which methods do the networking and the line numbers.

**After** (`docs/diagrams/adapter-after.drawio`): `GamePanelLAN` depends only on the `OpponentConnection` interface and implements `MoveListener`. `SocketConnectionAdapter` implements `OpponentConnection` and is the only class that points at the four Java classes. A note names the roles, and a second note shows how each call is translated.

## Why Adapter

Adapter fits because the problem is an interface mismatch: the game thinks in moves ("send this move", "tell me when the opponent moves"), while Java offers sockets, streams and blocking reads. The adapter converts one interface into the other in both directions, and the interface is shaped by what the client needs, not by the library. We considered three alternatives. A **class adapter**, where the adapter extends `Socket`, does not work: Java allows only one superclass, the code needs four classes, and it would expose every `Socket` method to the game. A **Facade** would give the networking code a simpler entry point, but its interface would still be shaped by the subsystem, and it would not translate method calls into callbacks. **Extracting private methods** inside `GamePanelLAN` would make the code tidier, but the panel would still depend on sockets directly. Our adapter does wrap several classes, which it shares with a Facade, but its purpose is converting one interface into another.

## Trade-offs and future work

The pattern adds a layer and three new types to read (`OpponentConnection`, `MoveListener`, `SocketConnectionAdapter`). The receiver thread is now hidden inside the adapter, so a reader must know that `onMoveReceived` and `onConnectionLost` run on a background thread, not on the Swing thread; this was already true in the original code. It is also harder to change a socket option from the game, because that now needs a new method on the adapter. In return, the game is decoupled from the transport: a new kind of connection only needs a class that implements `OpponentConnection`, with no change to `GamePanelLAN`. We tested the adapter on its own, without the GUI: two connections over localhost exchanged moves, and both reported a lost connection when one side closed.

Normal play is unchanged. We tested it with two copies of the game on one machine (127.0.0.1, port 1234): connecting, moves in both directions, turn locking, and the "Connection is lost" message when one window closes. Two rare error cases now behave differently. If joining and hosting both fail (for example, the port is used by another program), the original opened a board with no connection; now the game shows "Input data is not valid !" and lets the player try again. If sending a move fails, the original skipped `opponentTurn()` and let the same player move again; now the turn passes as normal and the error is printed. One bug from the original remains and is outside this pattern: if a third copy tries to join a game that already has two players, it freezes while waiting.

In Stage 3, the Strategy pattern will add a network opponent that holds an `OpponentConnection`. It will use this adapter without changes.
