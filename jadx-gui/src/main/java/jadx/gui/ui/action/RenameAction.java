package jadx.gui.ui.action;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.RenameDialog;

public final class RenameAction extends JNodeAction {
	private static final long serialVersionUID = -4680872086148463289L;

	public RenameAction(CodeArea codeArea) {
		super(ActionModel.CODE_RENAME, codeArea);
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		if (node == null) {
			return false;
		}
		if (node instanceof JRenameNode) {
			return ((JRenameNode) node).canRename();
		}
		return false;
	}

	@Override
	public void runAction(JNode node) {
		RenameDialog.rename(getCodeArea().getMainWindow(), (JRenameNode) node);
	}
}
