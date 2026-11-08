package jadx.gui.ui.tab;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JPopupMenu;

import jadx.api.gui.tree.ITreeNode;
import jadx.gui.ui.MainWindow;

abstract class QuickTabsParentNode extends QuickTabsBaseNode {
	private final TabsController tabsController;
	private final Map<ITreeNode, QuickTabsChildNode> childrenMap = new HashMap<>();

	protected QuickTabsParentNode(TabsController tabsController) {
		super();

		this.tabsController = tabsController;
	}

	public boolean addNode(ITreeNode node) {
		if (childrenMap.containsKey(node)) {
			return false;
		}
		QuickTabsChildNode childNode = new QuickTabsChildNode(node);
		childrenMap.put(node, childNode);
		add(childNode);
		return true;
	}

	public boolean removeJNode(ITreeNode node) {
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

	public QuickTabsChildNode getQuickTabsNode(ITreeNode node) {
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
