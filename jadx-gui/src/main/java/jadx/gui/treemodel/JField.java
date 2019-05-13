package jadx.gui.treemodel;

import javax.swing.*;

import jadx.api.JavaField;
import jadx.api.JavaNode;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class JField extends JNode {
	private static final long serialVersionUID = 1712572192106793359L;

	private static final ImageIcon ICON_FLD_DEF = UiUtils.openIcon("field_default_obj");
	private static final ImageIcon ICON_FLD_PRI = UiUtils.openIcon("field_private_obj");
	private static final ImageIcon ICON_FLD_PRO = UiUtils.openIcon("field_protected_obj");
	private static final ImageIcon ICON_FLD_PUB = UiUtils.openIcon("field_public_obj");

	private static final ImageIcon ICON_TRANSIENT = UiUtils.openIcon("transient_co");
	private static final ImageIcon ICON_VOLATILE = UiUtils.openIcon("volatile_co");

	private final transient JavaField field;
	private final transient JClass jParent;

	public JField(JavaField javaField, JClass jClass) {
		this.field = javaField;
		this.jParent = jClass;
	}

	@Override
	public JavaNode getJavaNode() {
		return field;
	}

	@Override
	public JClass getJParent() {
		return jParent;
	}

	@Override
	public JClass getRootClass() {
		return jParent.getRootClass();
	}

	@Override
	public int getLine() {
		return field.getDecompiledLine();
	}

	@Override
	public Icon getIcon() {
		AccessInfo af = field.getAccessFlags();
		OverlayIcon icon = UiUtils.makeIcon(af, ICON_FLD_PUB, ICON_FLD_PRI, ICON_FLD_PRO, ICON_FLD_DEF);
		if (af.isTransient()) {
			icon.add(ICON_TRANSIENT);
		}
		if (af.isVolatile()) {
			icon.add(ICON_VOLATILE);
		}
		return icon;
	}

	@Override
	public String makeString() {
		return UiUtils.typeFormat(field.getName(), field.getType());
	}

	@Override
	public String makeLongString() {
		return UiUtils.typeFormat(field.getFullName(), field.getType());
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JField && field.equals(((JField) o).field);
	}
}
