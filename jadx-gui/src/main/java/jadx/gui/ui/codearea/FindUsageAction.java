package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.UsageDialog;
import jadx.gui.utils.NLS;

public final class FindUsageAction extends AbstractPopupMenuAction {
	private static final long serialVersionUID = 4692546569977976384L;

	public FindUsageAction(CodePanel contentPanel, CodeArea codeArea, JClass jCls) {
		super(NLS.str("popup.find_usage"), contentPanel, codeArea, jCls);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			return;
		}
		MainWindow mainWindow = contentPanel.getTabbedPane().getMainWindow();
		JNode jNode = mainWindow.getCacheObject().getNodeCache().makeFrom(node);
		UsageDialog usageDialog = new UsageDialog(mainWindow, jNode);
		usageDialog.setVisible(true);
	}
}
