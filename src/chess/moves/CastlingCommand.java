package chess.moves;

public class CastlingCommand implements Command{
    private MoveCommand kingMove;
    private MoveCommand rookMove;

    public CastlingCommand(MoveCommand kingMove, MoveCommand rookMove){
        this.kingMove = kingMove;
        this.rookMove = rookMove;
    }

    public void execute(){
        kingMove.execute();
        rookMove.execute();
    }
    public void undo(){
        rookMove.undo();
        kingMove.undo();
    }

}
