package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;

import jadx.gui.utils.NLS;

public final class GoToDeclarationAction extends JNodeMenuAction {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(NLS.str("popup.go_to_declaration"), codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (jumpPos != null) {
			codeArea.getContentPanel().getTabbedPane().codeJump(jumpPos);
		}
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		super.popupMenuWillBecomeVisible(e);

		putValue(Action.SMALL_ICON, jumpPos == null ? null : jumpPos.getNode().getIcon());
	}
}
