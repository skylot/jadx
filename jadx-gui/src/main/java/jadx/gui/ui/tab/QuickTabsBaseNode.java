package jadx.gui.ui.tab;

import javax.swing.Icon;
import javax.swing.JPopupMenu;
import javax.swing.tree.DefaultMutableTreeNode;

import jadx.gui.ui.MainWindow;

abstract class QuickTabsBaseNode extends DefaultMutableTreeNode {
	JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return null;
	}

	Icon getIcon() {
		return null;
	}
}
