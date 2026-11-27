package chess.moves;

public interface Command {
    void execute();
    void undo();
}
