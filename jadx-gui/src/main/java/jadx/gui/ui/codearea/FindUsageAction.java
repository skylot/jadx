package jadx.gui.ui.codearea;

import java.awt.event.KeyEvent;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.dialog.UsageDialog;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FindUsageAction extends JNodeAction {
	private static final long serialVersionUID = 4692546569977976384L;

	public FindUsageAction(CodeArea codeArea) {
		super(NLS.str("popup.find_usage") + " (x)", codeArea);
		addKeyBinding(getKeyStroke(KeyEvent.VK_X, 0), "trigger usage");
	}

	@Override
	public void runAction(JNode node) {
		MainWindow mw = getCodeArea().getMainWindow();
		UsageDialog usageDialog = new UsageDialog(mw, node);
		mw.addLoadListener(loaded -> {
			if (!loaded) {
				usageDialog.dispose();
				return true;
			}
			return false;
		});
		usageDialog.setVisible(true);
	}
}
