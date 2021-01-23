package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.UsageDialog;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FindUsageAction extends JNodeMenuAction<JNode> {
	private static final long serialVersionUID = 4692546569977976384L;

	public FindUsageAction(CodeArea codeArea) {
		super(NLS.str("popup.find_usage") + " (x)", codeArea);
		KeyStroke key = getKeyStroke(KeyEvent.VK_X, 0);
		codeArea.getInputMap().put(key, "trigger usage");
		codeArea.getActionMap().put("trigger usage", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node = codeArea.getNodeUnderCaret();
				showUsageDialog();
			}
		});
	}

	private void showUsageDialog() {
		if (node != null) {
			UsageDialog usageDialog = new UsageDialog(codeArea.getMainWindow(), node);
			usageDialog.setVisible(true);
			node = null;
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showUsageDialog();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
