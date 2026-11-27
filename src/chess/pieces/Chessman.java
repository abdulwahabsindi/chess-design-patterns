package chess.pieces;


import chess.Position;
import chess.SideColor;

import java.util.ArrayList;


public abstract class Chessman implements Cloneable {

    public SideColor color;
    public boolean notMoved;
    public Position pos;
    public int value;
    public short imgSrc;

    /**
     * Prototype: returns a copy of this piece, of the same concrete class, with the same
     * state (including whether it has moved). The copy shares no mutable object with
     * the original.
     *
     * super.clone() copies every field, including subclass fields such as Pawn.startPosition.
     * Position is mutable, so the copy gets its own. A subclass that adds a mutable
     * object field must override this, call super.copy() and copy that field too.
     */
    public Chessman copy() {
        try {
            Chessman clone = (Chessman) super.clone();
            clone.pos = new Position(pos.x, pos.y);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError("Chessman implements Cloneable", e);
        }
    }

    protected abstract void loadImage();

    public abstract ArrayList<Position> getMoves(Chessman[][] board);
}
