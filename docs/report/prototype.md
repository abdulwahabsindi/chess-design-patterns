# Prototype (Creational)

## The problem

The AI picks its move by trying moves on a copy of the board, so it needs a copy of every piece without caring which kind each one is. The original code already had the shape of a Prototype: `Chessman` declares an abstract `copy(Chessman other)` (`Chessman.java`, line 18), each piece class overrides it, and `AI.minMax()` calls `chess.copy(chess)` on every piece (`AI.java`, lines 25–26). But the overrides did not copy anything. Each one built a new piece from constructor arguments, such as `return new Pawn(color, pos.x, pos.y);` (`Pawn.java`, lines 21–23). The same code appears six times, the parameter `other` is never used, and any state the constructor does not receive is lost. The constructors reset `notMoved` (`King.java` and `Rook.java`, line 14) and `startPosition` (`Pawn.java`, line 11), so a copied piece forgets that it has moved. On a test board, black had 15 legal moves but 17 on the AI's copy, including an illegal castle, and in some positions this crashed the AI.

## The solution

We kept the method but made it a real copy, written once:

- **Prototype: `Chessman`.** It implements Java's `Cloneable` interface and has one method, `copy()`, with no parameter. It calls `super.clone()`, which creates an object of the same concrete class and copies every field, including subclass fields such as `Pawn.startPosition`. `Position` can be changed in place, so the copy gets its own.
- **Concrete prototypes: the six piece classes.** Their `copy(Chessman other)` overrides are deleted; they inherit `copy()`.
- **Client: `AI`.** The new `AI.copyBoard(board)` calls `copy()` on every piece, and `AI.minMax()` uses it instead of the old inline code. Neither names a concrete piece class.

A new kind of piece now gets a correct copy without any copy code, and the client does not change. The copy keeps each piece's state, so the AI sees the same legal moves as the real board, and the crash no longer occurs on our test position.

## Class diagrams

**Before** (`docs/diagrams/prototype-before.drawio`): the abstract `Chessman` declares `copy(other)`, and each of the six piece classes overrides it by calling its own constructor. `AI` calls `copy(chess)` from inline code in `minMax`. The notes show the unused parameter and where the constructors reset `notMoved` and `startPosition`.

**After** (`docs/diagrams/prototype-after.drawio`): `Chessman` implements `Cloneable` and holds the only `copy()`; the six piece classes contain no copy code. `AI` depends only on `Chessman.copy()`, through `copyBoard`. A note names the roles: `Chessman` is the prototype, the six pieces are the concrete prototypes, and `AI` is the client.

## Why Prototype

Prototype fits because the AI needs copies of existing objects with their current state, and the object that knows its own class and state is the piece itself. We considered two alternatives. Factory Method, which our team used for creating pieces, builds a piece from scratch with `ChessmanFactory.getPiece(type, color, x, y)`, so it would lose the moved state exactly as the old code did; it suits new pieces, while Prototype suits duplicating pieces already in play. Copy constructors, where each piece overrides `copy()` with `return new Pawn(this);`, are another valid way to implement Prototype: every copied field is written out, but it takes six overrides. We chose one inherited `super.clone()` for less code. We named the method `copy()` rather than overriding `clone()` because the team's code already calls it, and the name hides the `Cloneable` mechanism, so a later switch to copy constructors would not change any caller.

## Trade-offs and future work

`super.clone()` copies fields shallowly: a field that holds an object is shared between the original and the copy. That is correct today, because the only such field, `pos`, is copied by hand. A future object field, such as a list of past moves, would be shared unless `copy()` is overridden; the comment on `copy()` says so. Making `Position` immutable would remove even that one line. In Stage 2, the pieces will share their images through a Flyweight, and there sharing is what we want, because an image never changes. Testing also revealed a separate bug in the AI's search, outside this pattern: when it takes back a castling move, it does not put the rook back (`AI.moveChessmanAI()`, lines 114–130). Giving each tried move a fresh board from `copyBoard` would fix it.
