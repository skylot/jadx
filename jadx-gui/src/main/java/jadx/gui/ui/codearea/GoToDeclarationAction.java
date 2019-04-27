package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.NLS;

public final class GoToDeclarationAction extends AbstractPopupMenuAction {
	private static final long serialVersionUID = -1186470538894941301L;

	public GoToDeclarationAction(CodePanel contentPanel, CodeArea codeArea, JClass jCls) {
		super(NLS.str("popup.go_to_declaration"), contentPanel, codeArea, jCls);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			return;
		}
		MainWindow mainWindow = contentPanel.getTabbedPane().getMainWindow();
		JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(node);
		mainWindow.getTabbedPane().codeJump(new JumpPosition(jNode, jNode.getLine()));
	}
}
