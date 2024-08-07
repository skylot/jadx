package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.action.ActionModel;

public final class GoToDeclarationAction extends JNodeAction {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(ActionModel.GOTO_DECLARATION, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		getCodeArea().getContentPanel().getTabsController().codeJump(node);
	}
}
