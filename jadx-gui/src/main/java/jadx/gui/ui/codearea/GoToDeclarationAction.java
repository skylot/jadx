package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;

import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class GoToDeclarationAction extends JNodeMenuAction<JumpPosition> {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(NLS.str("popup.go_to_declaration") + " (d)", codeArea);
		KeyStroke key = getKeyStroke(KeyEvent.VK_D, 0);
		codeArea.getInputMap().put(key, "trigger goto decl");
		codeArea.getActionMap().put("trigger goto decl", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node = getNodeByOffset(codeArea.getWordStart(codeArea.getCaretPosition()));
				doJump();
			}
		});
	}

	private void doJump() {
		if (node != null) {
			codeArea.getContentPanel().getTabbedPane().codeJump(node);
			node = null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		doJump();
	}

	@Nullable
	@Override
	public JumpPosition getNodeByOffset(int offset) {
		return codeArea.getDefPosForNodeAtOffset(offset);
	}
}
