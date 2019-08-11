package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.UsageDialog;
import jadx.gui.utils.NLS;

public final class FindUsageAction extends JNodeMenuAction {
	private static final long serialVersionUID = 4692546569977976384L;

	public FindUsageAction(CodeArea codeArea) {
		super(NLS.str("popup.find_usage"), codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (jumpPos == null) {
			return;
		}
		MainWindow mainWindow = codeArea.getContentPanel().getTabbedPane().getMainWindow();
		UsageDialog usageDialog = new UsageDialog(mainWindow, jumpPos.getNode());
		usageDialog.setVisible(true);
	}
}
