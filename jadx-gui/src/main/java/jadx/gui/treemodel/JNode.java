package jadx.gui.treemodel;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class JNode extends DefaultMutableTreeNode {

	public abstract JClass getJParent();

	/**
	 * Return top level JClass or self if already at top.
	 */
	public JClass getRootClass() {
		return null;
	}

	public abstract int getLine();

	public abstract void updateChilds();

	public abstract Icon getIcon();
}
