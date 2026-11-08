package jadx.gui.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jadx.api.gui.tree.IEditableTreeNode;

public abstract class JadxEditableTreeNode implements IEditableTreeNode {
	private volatile boolean changed = false;
	private final List<Consumer<Boolean>> changeListeners = new ArrayList<>();

	@Override
	public boolean isChanged() {
		return changed;
	}

	@Override
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
		listener.accept(changed);
	}
}
