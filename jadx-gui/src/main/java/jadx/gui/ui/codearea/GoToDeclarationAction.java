package jadx.gui.ui.codearea;

import java.awt.event.KeyEvent;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class GoToDeclarationAction extends JNodeAction {
	private static final long serialVersionUID = -1186470538894941301L;

	private transient @Nullable JumpPosition declPos;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(NLS.str("popup.go_to_declaration") + " (d)", codeArea);
		addKeyBinding(getKeyStroke(KeyEvent.VK_D, 0), "trigger goto decl");
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		if (node == null) {
			declPos = null;
			return false;
		}
		declPos = new JumpPosition(node);
		return true;
	}

	@Override
	public void runAction(JNode node) {
		if (declPos != null) {
			getCodeArea().getContentPanel().getTabbedPane().codeJump(declPos);
		}
	}
}
