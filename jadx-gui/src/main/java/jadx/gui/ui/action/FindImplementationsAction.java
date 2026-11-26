package jadx.gui.ui.action;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.ImplementationsDialog;
import jadx.gui.ui.dialog.MethodImplementationsFinder;

public final class FindImplementationsAction extends JNodeAction {
	private static final long serialVersionUID = 7418529630147258369L;

	public FindImplementationsAction(CodeArea codeArea) {
		super(ActionModel.FIND_IMPLEMENTATIONS, codeArea);
	}

	@Override
	public boolean isActionEnabled(@Nullable JNode node) {
		if (node instanceof JMethod) {
			return MethodImplementationsFinder.hasImplementations(((JMethod) node).getJavaMethod());
		}
		return false;
	}

	@Override
	public void runAction(JNode node) {
		if (node instanceof JMethod) {
			ImplementationsDialog.open(getCodeArea().getMainWindow(), (JMethod) node);
		}
	}
}
