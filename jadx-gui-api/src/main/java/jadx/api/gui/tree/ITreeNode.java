package jadx.api.gui.tree;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.tree.MutableTreeNode;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.IMainWindow;
import jadx.api.gui.code.JadxContentPanel;

public interface ITreeNode extends MutableTreeNode {

	/**
	 * Locale independent node identifier
	 */
	String getID();

	/**
	 * Node title
	 */
	String getName();

	/**
	 * Node icon (optional)
	 */
	@Nullable
	Icon getIcon();

	/**
	 * Node tooltip (optional)
	 */
	@Nullable
	String getTooltip();

	/**
	 * Create a UI component to be opened in tab on node click
	 */
	@Nullable
	JadxContentPanel getContentPanel(IMainWindow mainWindow);

	/**
	 * Optional method to disable content panel creation and opening
	 */
	default boolean hasContent() {
		return false;
	}

	/**
	 * Create a custom popup menu for node, additional entries may be added at the end
	 */
	default @Nullable JPopupMenu onTreePopupMenu(IMainWindow mainWindow) {
		return null;
	}

	/**
	 * Optional 'long' description
	 */
	default String getLongName() {
		return getName();
	}

	default boolean supportsQuickTabs() {
		return true;
	}
}
