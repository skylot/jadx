package jadx.gui.treemodel;

import jadx.api.JavaMethod;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.utils.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

public class JMethod extends DefaultMutableTreeNode implements JNode {
	private static final ImageIcon ICON_MTH_DEF = Utils.openIcon("methdef_obj");
	private static final ImageIcon ICON_MTH_PRI = Utils.openIcon("methpri_obj");
	private static final ImageIcon ICON_MTH_PRO = Utils.openIcon("methpro_obj");
	private static final ImageIcon ICON_MTH_PUB = Utils.openIcon("methpub_obj");

	private final JavaMethod mth;
	private final JClass jparent;

	public JMethod(JavaMethod javaMethod, JClass jClass) {
		this.mth = javaMethod;
		this.jparent = jClass;
	}

	@Override
	public void updateChilds() {
	}

	@Override
	public JClass getJParent() {
		return jparent;
	}

	@Override
	public int getLine() {
		return mth.getDecompiledLine();
	}

	@Override
	public Icon getIcon() {
		AccessInfo af = mth.getAccessFlags();
		if (af.isPublic()) {
			return ICON_MTH_PUB;
		} else if (af.isPrivate()) {
			return ICON_MTH_PRI;
		} else if (af.isProtected()) {
			return ICON_MTH_PRO;
		} else {
			return ICON_MTH_DEF;
		}
	}

	@Override
	public String toString() {
		return mth.getName();
	}
}
