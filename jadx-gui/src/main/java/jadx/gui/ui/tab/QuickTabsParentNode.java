package jadx.gui.ui.tab;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPopupMenu;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;

abstract class QuickTabsParentNode extends QuickTabsBaseNode {
	private final TabsController tabsController;
	private final Map<JNode, QuickTabsChildNode> childrenMap = new HashMap<>();

	protected QuickTabsParentNode(TabsController tabsController) {
		super();

		this.tabsController = tabsController;
	}

	public boolean addJNode(JNode node) {
		if (childrenMap.containsKey(node)) {
			return false;
		}
		QuickTabsChildNode childNode = new QuickTabsChildNode(node);
		childrenMap.put(node, childNode);
		add(childNode);
		return true;
	}

	public boolean removeJNode(JNode node) {
		QuickTabsChildNode childNode = childrenMap.remove(node);
		if (childNode == null) {
			return false;
		}
		remove(childNode);
		return true;
	}

	public void removeAllNodes() {
		removeAllChildren();
	}

	public QuickTabsChildNode getQuickTabsNode(JNode node) {
		return childrenMap.get(node);
	}

	public TabsController getTabsController() {
		return tabsController;
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
