package jadx.gui.treemodel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import jadx.api.gui.tree.IEditableTreeNode;

/**
 * Migrate to {@link jadx.gui.tree.JadxEditableTreeNode}
 */
@Deprecated
public abstract class JEditableNode extends JNode implements IEditableTreeNode {

	private volatile boolean changed = false;
	private final List<Consumer<Boolean>> changeListeners = new ArrayList<>();

	@Override
	public boolean isEditable() {
		return true;
	}

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
