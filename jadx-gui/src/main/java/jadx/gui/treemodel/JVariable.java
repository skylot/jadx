package jadx.gui.treemodel;

import javax.swing.Icon;

import jadx.api.JavaNode;
import jadx.api.JavaVariable;
import jadx.gui.utils.UiUtils;

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
	public int getPos() {
		return var.getDefPos();
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
	public String makeLongStringHtml() {
		return UiUtils.typeFormatHtml(var.getName(), var.getType());
	}

	@Override
	public String getTooltip() {
		String name = var.getName() + " (r" + var.getReg() + "v" + var.getSsa() + ")";
		String fullType = UiUtils.escapeHtml(var.getType().toString());
		return UiUtils.wrapHtml(fullType + ' ' + UiUtils.escapeHtml(name));
	}

	@Override
	public boolean canRename() {
		return true;
	}
}
