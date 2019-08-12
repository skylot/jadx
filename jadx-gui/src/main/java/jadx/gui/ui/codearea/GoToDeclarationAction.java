package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import org.jetbrains.annotations.Nullable;

import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

public final class GoToDeclarationAction extends JNodeMenuAction<JumpPosition> {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodeArea codeArea) {
		super(NLS.str("popup.go_to_declaration"), codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node != null) {
			codeArea.getContentPanel().getTabbedPane().codeJump(node);
		}
	}

	@Nullable
	@Override
	public JumpPosition getNodeByOffset(int offset) {
		return codeArea.getDefPosForNodeAtOffset(offset);
	}
}
