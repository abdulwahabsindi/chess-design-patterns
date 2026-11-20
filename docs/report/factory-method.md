# Factory Method (Creational)

## The problem

In the original code, every chess piece is created by hand, one `new` call per piece. `GamePanel.generatePieces()` (`GamePanel.java`, lines 124–153) holds 32 such calls, such as `new Rook(SideColor.WHITE, 0, 7)`. Piece creation is also spread across three places in two classes: the board set-up in `generatePieces()`, pawn promotion in `GamePanel.moveChessman()` (line 294), and a copy of the same promotion code in `AI.moveChessmanAI()` (`AI.java`, line 123). This makes the code hard to change. For example, the game always promotes a pawn to a queen, while real chess lets the player choose a queen, rook, bishop or knight. Adding that choice means editing the same logic in more than one class. If a developer changes `GamePanel` and forgets the copy in `AI`, the AI plans its moves for a different game from the one being played.

## The solution

We applied the Factory Method pattern by adding two new types: an enum, `ChessmanType`, and a class, `ChessmanFactory`. `ChessmanType` lists the six kinds of chessman (`PAWN`, `ROOK`, `KNIGHT`, `BISHOP`, `QUEEN`, `KING`). `ChessmanFactory.getPiece(type, color, x, y)` takes a chessman type, a colour and a board position, and returns a `Chessman` of that type, so callers never need to know which concrete class was created. Three methods now ask the factory instead of calling `new` themselves: `GamePanel.generatePieces()` for the starting board, and the pawn promotion in both `GamePanel.moveChessman()` and `AI.moveChessmanAI()`. Because every method in `AI` is static, `AI` holds its own factory in a `private static` field.

## Class diagrams

**Before** (`docs/diagrams/factory-before.drawio`): `GamePanel` creates all six piece classes directly, and `AI` creates `Queen` directly. Seven dashed «create» arrows leave two different classes, so the knowledge of how to build a piece is spread across the code.

**After** (`docs/diagrams/factory-after.drawio`): `GamePanel` and `AI` each hold one `ChessmanFactory`, and every «create» arrow now starts at the factory. The callers depend only on the factory and on the abstract `Chessman`, not on the six concrete classes. The factory uses `ChessmanType` to decide which piece to return.

## Why Factory Method

Factory Method fits because the problem is about creation: pieces were created in several places, and the code had to pick the right class (pawn, rook, queen...) at the moment it needed one. The factory puts all piece creation in one place and picks the class from what the caller asks for. We considered two other creational patterns. Prototype creates objects by copying an existing one, but promotion turns a pawn into a different kind of piece, so there is nothing to copy (Prototype does fit the AI's board copies, which is where our team applied it). Abstract Factory creates families of related objects that must match, such as a "classic" and a "cartoon" set of pieces; this game has no such families, and colour is just a value passed to the constructor. Finally, we improved on the textbook version, which selects the product with a `String`: our factory takes a `ChessmanType` enum, so only the six valid types can be passed, and a typo such as `QEEN` is caught when the code compiles instead of when the game runs.

## Trade-offs and future work

The pattern is not free. It adds two new files (`ChessmanFactory` and `ChessmanType`), and the project grew by about 30 lines overall. A reader who wants to know which object a call creates now has to open the factory instead of reading a `new` on the same line. In return, piece creation lives in one place, so a change to how pieces are built is made once. There is also a small cost in our design: `GamePanel` and `AI` each create their own factory object. This is harmless, because the factory holds no data, but a single shared factory (a Singleton) or a static `getPiece` method would remove the duplicate object. The clearest next step is the promotion rule: the game should let the player choose a queen, rook, bishop or knight. With the factory in place, that only means passing the chosen `ChessmanType` to `getPiece` instead of always passing `QUEEN`.
