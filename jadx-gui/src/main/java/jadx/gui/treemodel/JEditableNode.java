package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public abstract class JEditableNode extends JNode {

	private volatile boolean changed = false;
	private final List<Consumer<Boolean>> changeListeners = new ArrayList<>();

	public abstract void save(String newContent);

	@Override
	public boolean isEditable() {
		return true;
	}

	public boolean isChanged() {
		return changed;
	}

	public void setChanged(boolean changed) {
		if (this.changed != changed) {
			this.changed = changed;
			for (Consumer<Boolean> changeListener : changeListeners) {
				changeListener.accept(changed);
			}
		}
	}

	public void addChangeListener(Consumer<Boolean> listener) {
		changeListeners.add(listener);
	}
}
