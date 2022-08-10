package jadx.gui.ui.codearea;

import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.utils.NLS;

import static java.awt.event.KeyEvent.VK_N;
import static javax.swing.KeyStroke.getKeyStroke;

public final class RenameAction extends JNodeAction {
	private static final long serialVersionUID = -4680872086148463289L;

	public RenameAction(CodeArea codeArea) {
		super(NLS.str("popup.rename") + " (n)", codeArea);
		addKeyBinding(getKeyStroke(VK_N, 0), "trigger rename");
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
		RenameDialog.rename(getCodeArea().getMainWindow(), getCodeArea().getNode(), (JRenameNode) node);
	}
}
