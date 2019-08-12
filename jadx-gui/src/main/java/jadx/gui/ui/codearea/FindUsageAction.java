package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.UsageDialog;
import jadx.gui.utils.NLS;

public final class FindUsageAction extends JNodeMenuAction<JNode> {
	private static final long serialVersionUID = 4692546569977976384L;

	public FindUsageAction(CodeArea codeArea) {
		super(NLS.str("popup.find_usage"), codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			return;
		}
		UsageDialog usageDialog = new UsageDialog(codeArea.getMainWindow(), node);
		usageDialog.setVisible(true);
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
