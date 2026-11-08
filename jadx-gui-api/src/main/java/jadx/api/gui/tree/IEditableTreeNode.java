package jadx.api.gui.tree;

public interface IEditableTreeNode extends ITreeNode {

	void save(String newContent);

	boolean isChanged();

	void setChanged(boolean changed);
}
