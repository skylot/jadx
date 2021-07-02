package jadx.gui.treemodel;

import java.util.Iterator;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.ui.ContentPanel;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.UiUtils;

public class JMethod extends JNode {
	private static final long serialVersionUID = 3834526867464663751L;

	private static final ImageIcon ICON_MTH_DEF = UiUtils.openIcon("methdef_obj");
	private static final ImageIcon ICON_MTH_PRI = UiUtils.openIcon("methpri_obj");
	private static final ImageIcon ICON_MTH_PRO = UiUtils.openIcon("methpro_obj");
	private static final ImageIcon ICON_MTH_PUB = UiUtils.openIcon("methpub_obj");

	private static final ImageIcon ICON_CONSTRUCTOR = UiUtils.openIcon("constr_ovr");
	private static final ImageIcon ICON_SYNC = UiUtils.openIcon("synch_co");

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
	public int getLine() {
		return mth.getDecompiledLine();
	}

	@Override
	public ContentPanel getContentPanel(TabbedPane tabbedPane) {
		return new ClassCodeContentPanel(tabbedPane, this);
	}

	@Override
	public Icon getIcon() {
		AccessInfo accessFlags = mth.getAccessFlags();
		OverlayIcon icon = UiUtils.makeIcon(accessFlags, ICON_MTH_PUB, ICON_MTH_PRI, ICON_MTH_PRO, ICON_MTH_DEF);
		if (accessFlags.isConstructor()) {
			icon.add(ICON_CONSTRUCTOR);
		}
		if (accessFlags.isSynchronized()) {
			icon.add(ICON_SYNC);
		}
		return icon;
	}

	@Override
	public String getSyntaxName() {
		return SyntaxConstants.SYNTAX_STYLE_JAVA;
	}

	@Override
	public boolean canRename() {
		return !mth.getMethodNode().contains(AFlag.DONT_RENAME);
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
	public String makeDescString() {
		return UiUtils.typeStr(getReturnType()) + " " + makeBaseString();
	}

	@Override
	public boolean hasDescString() {
		return true;
	}

	@Override
	public int hashCode() {
		return mth.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		return this == o || o instanceof JMethod && mth.equals(((JMethod) o).mth);
	}
}
