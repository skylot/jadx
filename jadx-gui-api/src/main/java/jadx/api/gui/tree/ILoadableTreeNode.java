package jadx.api.gui.tree;

public interface ILoadableTreeNode {

	/**
	 * Load child nodes or node content on demand before expansion or search.
	 */
	void loadNode();
}
