package chess.moves;

import java.util.ArrayDeque;
import java.util.Deque;

public class MoveHistory {
    private Deque<Command> undoStack = new ArrayDeque<>();
    private Deque<Command> redoStack = new ArrayDeque<>();

    public void execute(Command c){
        c.execute();
        undoStack.push(c);
        redoStack.clear();
    }

    public void undo(){
        if (undoStack.isEmpty()) return;

        Command temp = undoStack.pop();
        temp.undo();
        redoStack.push(temp);
    }

    public void redo(){
        if (redoStack.isEmpty()) return;
        
        Command temp = redoStack.pop();
        temp.execute();
        undoStack.push(temp);
    }

    public boolean canUndo(){
        if(!undoStack.isEmpty()){
            return true;
        }
        return false;
    }

    public boolean canRedo(){
        if(!redoStack.isEmpty()){
            return true;
        }
        return false;
    }

    public int undoCount(){
        return undoStack.size();
    }

    public int redoCount(){
        return redoStack.size();
    }


    
    
}
