package jadx.gui.treemodel;

import javax.swing.*;

import jadx.api.JavaNode;
import jadx.api.JavaVariable;

public class JVariable extends JNode {
	private static final long serialVersionUID = -3002100457834453783L;

	JClass cls;
	JavaVariable var;

	public JVariable(JavaVariable var, JClass cls) {
		this.cls = cls;
		this.var = var;
	}

	public JavaVariable getJavaVarNode() {
		return (JavaVariable) getJavaNode();
	}

	@Override
	public JClass getRootClass() {
		return cls;
	}

	@Override
	public JavaNode getJavaNode() {
		return var;
	}

	@Override
	public JClass getJParent() {
		return cls;
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
	public boolean canRename() {
		return true;
	}

}
