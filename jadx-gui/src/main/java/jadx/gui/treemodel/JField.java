package jadx.gui.treemodel;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import jadx.api.JavaField;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
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

	public JavaField getJavaField() {
		return (JavaField) getJavaNode();
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
	public boolean canRename() {
		return !field.getFieldNode().contains(AFlag.DONT_RENAME);
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
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public String makeString() {
		return UiUtils.typeFormat(field.getName(), field.getType());
	}

	@Override
	public String makeStringHtml() {
		return UiUtils.typeFormatHtml(field.getName(), field.getType());
	}

	@Override
	public String makeLongString() {
		return UiUtils.typeFormat(field.getFullName(), field.getType());
	}

	@Override
	public String makeLongStringHtml() {
		return UiUtils.typeFormatHtml(field.getFullName(), field.getType());
	}

	@Override
	public String makeDescString() {
		return UiUtils.typeStr(field.getType()) + " " + field.getName();
	}

	@Override
	public boolean hasDescString() {
		return true;
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
