package jadx.gui.treemodel;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;

public abstract class JNode extends DefaultMutableTreeNode {
	private static final long serialVersionUID = 1337L; // TODO: Create your own serialvers with serialver/eclipse
	public static JNode makeFrom(JavaNode node) {
		if (node instanceof JavaClass) {
			JClass p = (JClass) makeFrom(node.getDeclaringClass());
			return new JClass((JavaClass) node, p);
		}
		if (node instanceof JavaMethod) {
			JavaMethod mth = (JavaMethod) node;
			return new JMethod(mth, new JClass(mth.getDeclaringClass()));
		}
		if (node instanceof JavaField) {
			JavaField fld = (JavaField) node;
			return new JField(fld, new JClass(fld.getDeclaringClass()));
		}
		if (node == null) {
			return null;
		}
		throw new JadxRuntimeException("Unknown type for JavaNode: " + node.getClass());
	}

	public abstract JClass getJParent();

	/**
	 * Return top level JClass or self if already at top.
	 */
	public JClass getRootClass() {
		return null;
	}

	public abstract int getLine();

	public abstract Icon getIcon();

	public abstract String makeString();

	public String makeLongString() {
		return makeString();
	}

	@Override
	public String toString() {
		return makeString();
	}
}
