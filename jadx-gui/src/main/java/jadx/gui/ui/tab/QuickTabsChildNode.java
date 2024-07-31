package jadx.gui.ui.tab;

import javax.swing.JPopupMenu;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

public class QuickTabsChildNode extends QuickTabsBaseNode {
	private final JNode node;

	public QuickTabsChildNode(JNode node) {
		this.node = node;
	}

	@Override
	public String toString() {
		return node.toString();
	}

	public JNode getJNode() {
		return node;
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return getJNode().onTreePopupMenu(mainWindow);
	}
}
