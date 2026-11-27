package chess.moves;
import chess.pieces.Chessman;
import chess.pieces.Pawn;
import chess.Position;

public class MoveCommand implements Command {
    private Chessman[][] board;
    private Chessman piece;
    private Position from;
    private Position to;
    private Chessman promotion;
    private Chessman captured;
    private boolean oldNotMoved;
    private boolean oldStartPosition;




    public MoveCommand(Chessman[][] board, Chessman piece, Position from, Position to, Chessman promotion){
        this.board = board;
        this.piece = piece;
        this.from = from;
        this.to = to;
        this.promotion = promotion;
    }

    public void execute(){
        captured = board[to.x][to.y];

        if(piece instanceof Pawn){
            oldStartPosition = ((Pawn)piece).startPosition;
            ((Pawn)piece).startPosition = false;
        }

        oldNotMoved = piece.notMoved;

        Chessman landing;
        if(promotion != null){
            landing = promotion;
        }
        else{
            landing = piece;
        }

        board[to.x][to.y] = landing;
        board[from.x][from.y] = null;
        landing.pos = to;
        landing.notMoved = false;

    }
    public void undo(){
        board[from.x][from.y] = piece;
        piece.pos = from;
        piece.notMoved = oldNotMoved;
        if(piece instanceof Pawn){
            ((Pawn)piece).startPosition = oldStartPosition;
        }
        board[to.x][to.y] = captured;
    }
}
