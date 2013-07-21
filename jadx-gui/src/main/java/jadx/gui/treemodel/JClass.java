package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.gui.Utils;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

public class JClass extends DefaultMutableTreeNode implements JNode {

	private final JavaClass cls;

	public JClass(JavaClass cls) {
		this.cls = cls;
	}

	public JavaClass getCls() {
		return cls;
	}

	@Override
	public Icon getIcon() {
		return Utils.openIcon("class_obj");
	}

	@Override
	public String toString() {
		return cls.getShortName();
	}
}
