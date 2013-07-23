package jadx.gui.treemodel;

import javax.swing.Icon;

public interface JNode {

	JClass getJParent();

	int getLine();

	void updateChilds();

	Icon getIcon();
}
