package jadx.gui.treemodel;

import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import jadx.api.JavaNode;
import jadx.api.JavaVariable;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.deobf.NameMapper;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;

public class JVariable extends JNode implements JRenameNode {
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
	public boolean disableHtml() {
		return false;
	}

	@Override
	public String getTooltip() {
		String name = var.getName() + " (r" + var.getReg() + "v" + var.getSsa() + ")";
		String fullType = UiUtils.escapeHtml(var.getType().toString());
		return UiUtils.wrapHtml(fullType + ' ' + UiUtils.escapeHtml(name));
	}

	@Override
	public boolean canRename() {
		return var.getName() != null;
	}

	@Override
	public String getTitle() {
		return makeLongStringHtml();
	}

	@Override
	public boolean isValidName(String newName) {
		return NameMapper.isValidIdentifier(newName);
	}

	@Override
	public ICodeRename buildCodeRename(String newName, Set<ICodeRename> renames) {
		return new JadxCodeRename(JadxNodeRef.forMth(var.getMth()), JadxCodeRef.forVar(var), newName);
	}

	@Override
	public void removeAlias() {
		var.removeAlias();
	}

	@Override
	public void addUpdateNodes(List<JavaNode> toUpdate) {
		toUpdate.add(var.getMth());
	}

	@Override
	public void reload(MainWindow mainWindow) {
	}
}
