package chess.pieces;
import chess.SideColor;

public class ChessmanFactory {
    public Chessman getPiece(ChessmanType type, SideColor color, int x, int y){
        
        if (type == ChessmanType.BISHOP){
            return new Bishop(color, x, y);
        }
        else if(type == ChessmanType.KING){
            return new King(color, x, y);
        }
        else if(type == ChessmanType.KNIGHT){
            return new Knight(color, x, y);
        }
        else if(type == ChessmanType.PAWN){
                return new Pawn(color, x, y);
        }
        else if(type == ChessmanType.QUEEN){
                return new Queen(color, x, y);
        }
        else if(type == ChessmanType.ROOK){
                return new Rook(color, x, y);
        }
        else{
            throw new IllegalArgumentException("Unknown Chessman Type: " + type);
        }
    }
}
