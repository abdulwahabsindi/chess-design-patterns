# Flyweight (Structural)

## The problem

The board shows 32 pieces but only 12 different pictures, one per type and colour. The original code already shared them, through numbers that only work by convention. `GamePanel.loadImages()` reads the 12 PNG files into the array `imgTable` at fixed slots, for example the white pawn at slot 5 (`GamePanel.java`, lines 155–180). Each piece class has its own `loadImage()` that stores a slot number in `Chessman.imgSrc` (`Chessman.java`, lines 16 and 20): `Pawn` sets 5 for white and 11 for black (`Pawn.java`, lines 26–30). `paintComponent` then draws `imgTable[piecesBoard[i][j].imgSrc]` (`GamePanel.java`, line 80). Nothing connects the 5 in `Pawn` to slot 5 in `GamePanel`, so renumbering either side draws the wrong picture without any error. Also, `loadImages()` runs in the `GamePanel` constructor (line 56), and `Menu` creates a new panel for every game (`Menu.java`, lines 53, 63 and 76), so each game reads all 12 files from disk again.

## The solution

We made the sharing explicit with the Flyweight pattern, in a new package, `chess.graphics`:

- **Flyweight: `PieceSprite`.** It holds one picture, the intrinsic state, which is the same for every piece that uses it and never changes. The square is the extrinsic state, passed in by the caller: `draw(g, x, y, size)`. A sprite reads its PNG the first time it is drawn.
- **Flyweight factory: `PieceSprites`.** `get(ChessmanType, SideColor)` returns the cached sprite, or creates it on the first request. The `PieceSprite` constructor is package-private, and only `PieceSprites` calls it. One cache serves the whole program, so at most 12 sprites exist. It is a `ConcurrentHashMap` because the AI and LAN threads create a queen when a pawn promotes.
- **Clients: the six piece classes and `GamePanel`.** Each piece constructor fetches its sprite, for example `sprite = PieceSprites.get(ChessmanType.PAWN, color);`, and `GamePanel` calls `sprite.draw(...)` with the square's position.

We deleted `imgSrc`, `imgTable`, `loadImages()` and all seven `loadImage()` methods. The mapping from piece to file name is now in one place, a `switch` over `ChessmanType` without a `default` case, so a new piece type without a picture does not compile. Renders of the starting position and of a pawn promotion are identical pixel for pixel before and after the change.

## Class diagrams

**Before** (`docs/diagrams/flyweight-before.drawio`): `GamePanel` owns `imgTable` and `loadImages()` and reads `imgSrc` from each `Chessman`. Every piece class overrides `loadImage()`. The notes show Pawn's slot numbers and the per-game loading.

**After** (`docs/diagrams/flyweight-after.drawio`): many `Chessman` objects point to one `PieceSprite`, and `PieceSprites` caches at most 12. `GamePanel` calls `draw` and supplies the position. A note names the roles and the intrinsic and extrinsic state.

## Why Flyweight

Flyweight fits because many objects need the same unchanging data, and that data separates cleanly from the part that changes: the picture belongs to the type and colour, the square to the moment of drawing. It also works with the Stage 1 patterns. `ChessmanType`, from the Factory Method, is the cache key, and Prototype's `super.clone()` copies the `sprite` reference, so the AI's board copies share the sprites with no extra code.

We considered keeping drawing code out of the pieces: `GamePanel` would call `PieceSprites.get(piece.getType(), piece.color)` while painting. That is still Flyweight, but the piece would no longer carry its own picture. A static image array in `GamePanel` would stop the reloading but keep the magic numbers. The textbook pattern also puts an interface above the concrete flyweights; we have one kind of sprite and no unshared ones, so one class is enough.

## Trade-offs and future work

The pieces now reference a drawing class, and `chess.pieces` and `chess.graphics` depend on each other. The original had the same link, hidden as `imgSrc`; now it is typed. The memory gain is modest, since the original also kept 12 images per game, not 32. The gains are safe sharing and reading each file once per program instead of once per game. Lazy loading means the image field cannot be `final`. It changes once, on the Swing event thread, so it needs no locking, and the unit tests that build pieces never touch the disk. An unreadable image now stops the game, naming the file, where the original printed "Error" and drew nothing. If the game later offers several piece sets, the set would join the cache key, and the `getType()` design would become cleaner, because choosing a picture would then be the screen's job.
