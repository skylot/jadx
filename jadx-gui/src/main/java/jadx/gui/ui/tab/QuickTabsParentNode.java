package jadx.gui.ui.tab;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPopupMenu;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

abstract class QuickTabsParentNode extends QuickTabsBaseNode {
	private final TabbedPane tabbedPane;
	private final Map<JNode, QuickTabsChildNode> childrenMap = new HashMap<>();

	protected QuickTabsParentNode(TabbedPane tabbedPane) {
		super();

		this.tabbedPane = tabbedPane;
	}

	public void addJNode(JNode node) {
		if (childrenMap.containsKey(node)) {
			return;
		}
		QuickTabsChildNode childNode = new QuickTabsChildNode(node);
		childrenMap.put(node, childNode);
		add(childNode);
	}

	public void removeJNode(JNode node) {
		QuickTabsChildNode childNode = childrenMap.remove(node);
		if (childNode == null) {
			return;
		}
		remove(childNode);
	}

	public QuickTabsChildNode getQuickTabsNode(JNode node) {
		return childrenMap.get(node);
	}

	public TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	abstract String getTitle();

	@Override
	public String toString() {
		return getTitle();
	}

	@Override
	JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return super.onTreePopupMenu(mainWindow);
	}
}
