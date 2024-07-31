package jadx.gui.ui.tab;

import java.util.HashMap;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import jadx.gui.treemodel.JNode;

abstract class QuickTabsParentNode extends DefaultMutableTreeNode {
	private final Map<JNode, QuickTabsChildNode> childrenMap = new HashMap<>();

	protected QuickTabsParentNode(TabbedPane tabbedPane) {
		super();
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

	abstract String getTitle();

	@Override
	public String toString() {
		return getTitle();
	}
}
