package jadx.gui.treemodel;

import java.util.Comparator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;

import jadx.api.JavaField;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.utils.Icons;
import jadx.gui.utils.UiUtils;

public class JField extends JNode {
	private static final long serialVersionUID = 1712572192106793359L;

	private static final ImageIcon ICON_FLD_PRI = UiUtils.openSvgIcon("nodes/privateField");
	private static final ImageIcon ICON_FLD_PRO = UiUtils.openSvgIcon("nodes/protectedField");
	private static final ImageIcon ICON_FLD_PUB = UiUtils.openSvgIcon("nodes/publicField");
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
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return RenameDialog.buildRenamePopup(mainWindow, this);
	}

	@Override
	public Icon getIcon() {
		AccessInfo af = field.getAccessFlags();
		return UiUtils.makeIcon(af, ICON_FLD_PUB, ICON_FLD_PRI, ICON_FLD_PRO, Icons.FIELD);
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
	public String getTooltip() {
		String fullType = UiUtils.escapeHtml(field.getType().toString());
		return UiUtils.wrapHtml(fullType + ' ' + UiUtils.escapeHtml(field.getName()));
	}

	@Override
	public String makeDescString() {
		return UiUtils.typeStr(field.getType()) + " " + field.getName();
	}

	@Override
	public boolean disableHtml() {
		return false;
	}

	@Override
	public boolean hasDescString() {
		return false;
	}

	@Override
	public int hashCode() {
		return field.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JField && field.equals(((JField) o).field);
	}

	private static final Comparator<JField> COMPARATOR = Comparator
			.comparing(JField::getJParent)
			.thenComparing(JNode::getName)
			.thenComparingInt(JField::getPos);

	public int compareToFld(@NotNull JField other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public int compareTo(@NotNull JNode other) {
		if (other instanceof JField) {
			return compareToFld(((JField) other));
		}
		return super.compareTo(other);
	}
}
