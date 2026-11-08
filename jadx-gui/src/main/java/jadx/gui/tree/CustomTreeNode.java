package jadx.gui.tree;

import jadx.api.gui.tree.BaseTreeNodeEnum;
import jadx.api.gui.tree.ITreeNode;

public class CustomTreeNode {
	private final BaseTreeNodeEnum parent;
	private final ITreeNode node;

	public CustomTreeNode(BaseTreeNodeEnum parent, ITreeNode node) {
		this.parent = parent;
		this.node = node;
	}

	public ITreeNode getNode() {
		return node;
	}

	public BaseTreeNodeEnum getParent() {
		return parent;
	}

	@Override
	public String toString() {
		return "CustomTreeNode{node=" + node + ", path=" + parent + '}';
	}
}
