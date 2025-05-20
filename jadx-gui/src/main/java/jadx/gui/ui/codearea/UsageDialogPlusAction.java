package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JNodeAction;
import jadx.gui.ui.dialog.UsageDialogPlus;

public final class UsageDialogPlusAction extends JNodeAction {
	private static final long serialVersionUID = 4692546569977976384L;

	public UsageDialogPlusAction(CodeArea codeArea) {
		super(ActionModel.FIND_USAGE_PLUS, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		UsageDialogPlus.open(getCodeArea().getMainWindow(), node);
	}
}
