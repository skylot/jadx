package jadx.gui.tree;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.IMainWindow;
import jadx.api.gui.code.JadxContentPanel;
import jadx.api.gui.tree.ITreeNode;

/**
 * Base implementation of {@link ITreeNode}
 */
public abstract class JadxTreeNode extends DefaultMutableTreeNode implements ITreeNode {

	@Override
	public String getID() {
		return this.getClass().getName();
	}

	@Override
	public @Nullable String getTooltip() {
		return null;
	}

	@Override
	public @Nullable JadxContentPanel getContentPanel(IMainWindow mainWindow) {
		return null;
	}

	@Override
	public @Nullable Icon getIcon() {
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}
}
