package jadx.gui.ui.codearea;

import java.awt.event.KeyEvent;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class GoToDeclarationAction extends JNodeAction {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(NLS.str("popup.go_to_declaration") + " (d)", codeArea);
		addKeyBinding(getKeyStroke(KeyEvent.VK_D, 0), "trigger goto decl");
	}

	@Override
	public void runAction(JNode node) {
		getCodeArea().getContentPanel().getTabbedPane().codeJump(node);
	}
}
