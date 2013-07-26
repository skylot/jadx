package jadx.gui.treemodel;

import jadx.api.JavaMethod;
import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.gui.utils.OverlayIcon;
import jadx.gui.utils.Utils;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Iterator;

public class JMethod extends DefaultMutableTreeNode implements JNode {
	private static final ImageIcon ICON_MTH_DEF = Utils.openIcon("methdef_obj");
	private static final ImageIcon ICON_MTH_PRI = Utils.openIcon("methpri_obj");
	private static final ImageIcon ICON_MTH_PRO = Utils.openIcon("methpro_obj");
	private static final ImageIcon ICON_MTH_PUB = Utils.openIcon("methpub_obj");

	private static final ImageIcon ICON_CONSTRUCTOR = Utils.openIcon("constr_ovr");
	private static final ImageIcon ICON_SYNC = Utils.openIcon("synch_co");

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
		AccessInfo accessFlags = mth.getAccessFlags();
		OverlayIcon icon = Utils.makeIcon(accessFlags, ICON_MTH_PUB, ICON_MTH_PRI, ICON_MTH_PRO, ICON_MTH_DEF);
		if(accessFlags.isConstructor()) icon.add(ICON_CONSTRUCTOR);
		if (accessFlags.isSynchronized()) icon.add(ICON_SYNC);
		return icon;
	}

	@Override
	public String toString() {
		if (mth.isClassInit()) {
			return "{...}";
		}

		StringBuilder base = new StringBuilder();
		base.append(mth.getName());
		base.append('(');
		for (Iterator<ArgType> it = mth.getArguments().iterator(); it.hasNext(); ) {
			base.append(Utils.typeStr(it.next()));
			if(it.hasNext())
				base.append(", ");
		}
		base.append(')');
		return Utils.typeFormat(base.toString(), mth.getReturnType());
	}
}
