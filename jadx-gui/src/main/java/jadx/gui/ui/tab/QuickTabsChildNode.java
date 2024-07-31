package jadx.gui.ui.tab;

import javax.swing.tree.DefaultMutableTreeNode;

import jadx.gui.treemodel.JNode;

public class QuickTabsChildNode extends DefaultMutableTreeNode {
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
}
