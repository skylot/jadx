package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.gui.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

public class JPackage extends DefaultMutableTreeNode implements JNode {
	private static final ImageIcon PACKAGE_ICON = Utils.openIcon("package_obj");

	private final JavaPackage pkg;

	public JPackage(JavaPackage pkg) {
		this.pkg = pkg;

		for (JavaClass javaClass : pkg.getClasses()) {
			add(new JClass(javaClass));
		}
	}

	public JavaPackage getPkg() {
		return pkg;
	}

	@Override
	public Icon getIcon() {
		return PACKAGE_ICON;
	}

	@Override
	public String toString() {
		return pkg.getName();
	}
}
