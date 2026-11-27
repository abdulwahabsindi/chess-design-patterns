package chess.graphics;

import chess.SideColor;
import chess.pieces.ChessmanType;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flyweight factory: the only way to get a PieceSprite. It keeps one sprite per piece
 * type and colour for the whole program, so at most 12 exist, and every game reuses them.
 */
public final class PieceSprites {

    private static final String FOLDER = "/chess/resources/64/";

    // Concurrent because the AI and LAN threads create a queen when a pawn promotes.
    private static final Map<String, PieceSprite> cache = new ConcurrentHashMap<>();

    private PieceSprites() {
    }

    public static PieceSprite get(ChessmanType type, SideColor color) {
        return cache.computeIfAbsent(fileName(type, color), file -> new PieceSprite(FOLDER + file));
    }

    private static String fileName(ChessmanType type, SideColor color) {
        String piece = switch (type) {
            case KING -> "Kg";
            case QUEEN -> "Q";
            case ROOK -> "R";
            case BISHOP -> "B";
            case KNIGHT -> "Kt";
            case PAWN -> "P";
        };
        return piece + (color == SideColor.WHITE ? "W" : "B") + ".png";
    }
}
