package jadx.gui.ui.hexeditor.buffer;

import java.util.ArrayList;
import java.util.List;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class ByteBufferHistory {
	private final List<ByteBufferAction> actions = new ArrayList<ByteBufferAction>();
	private int actionPointer = 0;

	public boolean canUndo() {
		return actionPointer > 0;
	}

	public boolean canRedo() {
		return actionPointer < actions.size();
	}

	public ByteBufferAction getUndoAction() {
		return (actionPointer > 0) ? actions.get(actionPointer - 1) : null;
	}

	public ByteBufferAction getRedoAction() {
		return (actionPointer < actions.size()) ? actions.get(actionPointer) : null;
	}

	public String getUndoActionName() {
		return (actionPointer > 0) ? actions.get(actionPointer - 1).getName() : null;
	}

	public String getRedoActionName() {
		return (actionPointer < actions.size()) ? actions.get(actionPointer).getName() : null;
	}

	public void undo() {
		if (actionPointer > 0) {
			actionPointer--;
			actions.get(actionPointer).undo();
		}
	}

	public void redo() {
		if (actionPointer < actions.size()) {
			actions.get(actionPointer).redo();
			actionPointer++;
		}
	}

	public void add(ByteBufferAction action) {
		actions.subList(actionPointer, actions.size()).clear();
		actions.add(action);
		actionPointer = actions.size();
	}

	public void clear() {
		actions.clear();
		actionPointer = 0;
	}
}
