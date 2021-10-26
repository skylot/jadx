package jadx.gui.treemodel;

import javax.swing.Icon;

import jadx.api.JavaNode;
import jadx.api.JavaVariable;

public class JVariable extends JNode {
	private static final long serialVersionUID = -3002100457834453783L;

	private final JMethod jMth;
	private final JavaVariable var;

	public JVariable(JMethod jMth, JavaVariable var) {
		this.jMth = jMth;
		this.var = var;
	}

	public JavaVariable getJavaVarNode() {
		return var;
	}

	@Override
	public JavaNode getJavaNode() {
		return var;
	}

	@Override
	public JClass getRootClass() {
		return jMth.getRootClass();
	}

	@Override
	public JClass getJParent() {
		return jMth.getJParent();
	}

	@Override
	public Icon getIcon() {
		return null;
	}

	@Override
	public String makeString() {
		return var.getName();
	}

	@Override
	public String makeLongString() {
		return var.getFullName();
	}

	@Override
	public boolean canRename() {
		return true;
	}
}
