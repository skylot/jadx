package jadx.gui.treemodel;

import java.util.Comparator;
import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JPopupMenu;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.jetbrains.annotations.NotNull;

import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.utils.Icons;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class JMethod extends JNode {
	private static final long serialVersionUID = 3834526867464663751L;
	private static final ImageIcon ICON_METHOD_ABSTRACT = UiUtils.openSvgIcon("nodes/abstractMethod");
	private static final ImageIcon ICON_METHOD_PRIVATE = UiUtils.openSvgIcon("nodes/privateMethod");
	private static final ImageIcon ICON_METHOD_PROTECTED = UiUtils.openSvgIcon("nodes/protectedMethod");
	private static final ImageIcon ICON_METHOD_PUBLIC = UiUtils.openSvgIcon("nodes/publicMethod");
	private static final ImageIcon ICON_METHOD_CONSTRUCTOR = UiUtils.openSvgIcon("nodes/constructorMethod");
	private static final ImageIcon ICON_METHOD_SYNC = UiUtils.openSvgIcon("nodes/methodReference");

	private final transient JavaMethod mth;
	private final transient JClass jParent;

	public JMethod(JavaMethod javaMethod, JClass jClass) {
		this.mth = javaMethod;
		this.jParent = jClass;
	}

	@Override
	public JavaNode getJavaNode() {
		return mth;
	}

	public JavaMethod getJavaMethod() {
		return mth;
	}

	@Override
	public JClass getJParent() {
		return jParent;
	}

	public ArgType getReturnType() {
		return mth.getReturnType();
	}

	@Override
	public JClass getRootClass() {
		return jParent.getRootClass();
	}

	@Override
	public Icon getIcon() {
		AccessInfo accessFlags = mth.getAccessFlags();
		Icon icon = Icons.METHOD;
		if (accessFlags.isAbstract()) {
			icon = ICON_METHOD_ABSTRACT;
		}
		if (accessFlags.isConstructor()) {
			icon = ICON_METHOD_CONSTRUCTOR;
		}
		if (accessFlags.isPublic()) {
			icon = ICON_METHOD_PUBLIC;
		}
		if (accessFlags.isPrivate()) {
			icon = ICON_METHOD_PRIVATE;
		}
		if (accessFlags.isProtected()) {
			icon = ICON_METHOD_PROTECTED;
		}
		if (accessFlags.isSynchronized()) {
			icon = ICON_METHOD_SYNC;
		}

		OverlayIcon overIcon = new OverlayIcon(icon);
		if (accessFlags.isFinal()) {
			overIcon.add(Icons.FINAL);
		}
		if (accessFlags.isStatic()) {
			overIcon.add(Icons.STATIC);
		}

		return overIcon;
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public boolean canRename() {
		if (mth.isClassInit()) {
			return false;
		}
		return !mth.getMethodNode().contains(AFlag.DONT_RENAME);
	}

	@Override
	public JPopupMenu onTreePopupMenu(MainWindow mainWindow) {
		return RenameDialog.buildRenamePopup(mainWindow, this);
	}

	String makeBaseString() {
		if (mth.isClassInit()) {
			return "{...}";
		}
		StringBuilder base = new StringBuilder();
		if (mth.isConstructor()) {
			base.append(mth.getDeclaringClass().getName());
		} else {
			base.append(mth.getName());
		}
		base.append('(');
		for (Iterator<ArgType> it = mth.getArguments().iterator(); it.hasNext();) {
			base.append(UiUtils.typeStr(it.next()));
			if (it.hasNext()) {
				base.append(", ");
			}
		}
		base.append(')');
		return base.toString();
	}

	@Override
	public String getName() {
		return mth.getName();
	}

	@Override
	public String makeString() {
		return UiUtils.typeFormat(makeBaseString(), getReturnType());
	}

	@Override
	public String makeStringHtml() {
		return UiUtils.typeFormatHtml(makeBaseString(), getReturnType());
	}

	@Override
	public String makeLongString() {
		String name = mth.getDeclaringClass().getFullName() + '.' + makeBaseString();
		return UiUtils.typeFormat(name, getReturnType());
	}

	@Override
	public String makeLongStringHtml() {
		String name = mth.getDeclaringClass().getFullName() + '.' + makeBaseString();
		return UiUtils.typeFormatHtml(name, getReturnType());
	}

	@Override
	public boolean disableHtml() {
		return false;
	}

	@Override
	public String makeDescString() {
		return UiUtils.typeStr(getReturnType()) + " " + makeBaseString();
	}

	@Override
	public boolean hasDescString() {
		return false;
	}

	@Override
	public int getPos() {
		return mth.getDefPos();
	}

	@Override
	public int hashCode() {
		return mth.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JMethod && mth.equals(((JMethod) o).mth);
	}

	private static final Comparator<JMethod> COMPARATOR = Comparator
			.comparing(JMethod::getJParent)
			.thenComparing(jMethod -> jMethod.mth.getMethodNode().getMethodInfo().getShortId())
			.thenComparingInt(JMethod::getPos);

	public int compareToMth(@NotNull JMethod other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public int compareTo(@NotNull JNode other) {
		if (other instanceof JMethod) {
			return compareToMth(((JMethod) other));
		}
		if (other instanceof JClass) {
			JClass cls = (JClass) other;
			int cmp = jParent.compareToCls(cls);
			if (cmp != 0) {
				return cmp;
			}
			return 1;
		}
		return super.compareTo(other);
	}
}
