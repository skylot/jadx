package jadx.gui.treemodel;

import jadx.api.JavaField;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;

public class JField extends DefaultMutableTreeNode implements JNode  {
	private static final ImageIcon ICON_FLD_DEF = Utils.openIcon("field_default_obj");
	private static final ImageIcon ICON_FLD_PRI = Utils.openIcon("field_private_obj");
	private static final ImageIcon ICON_FLD_PRO = Utils.openIcon("field_protected_obj");
	private static final ImageIcon ICON_FLD_PUB = Utils.openIcon("field_public_obj");

	private final JavaField field;
	private final JClass jParent;

	public JField(JavaField javaField, JClass jClass) {
		this.field = javaField;
		this.jParent = jClass;
	}

	@Override
	public void updateChilds() {
	}

	@Override
	public JClass getJParent() {
		return jParent;
	}

	@Override
	public int getLine() {
		return field.getDecompiledLine();
	}

	@Override
	public Icon getIcon() {
		AccessInfo af = field.getAccessFlags();
		if(af.isPublic()){
			return ICON_FLD_PUB;
		} else if(af.isPrivate()) {
			return ICON_FLD_PRI;
		} else if(af.isProtected()) {
			return ICON_FLD_PRO;
		} else {
			return ICON_FLD_DEF;
		}
	}

	@Override
	public String toString() {
		return field.getName();
	}
}
