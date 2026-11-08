package jadx.api.gui.tree;

public interface IJadxTree {

	void addNode(BaseTreeNodeEnum parent, ITreeNode node);

	/**
	 * Open tab with tree node content
	 * (Same as clicking that node in tree)
	 */
	void openTreeNodeContent(ITreeNode node);
}
