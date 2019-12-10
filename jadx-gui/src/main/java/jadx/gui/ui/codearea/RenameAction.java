package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.RenameDialog;
import jadx.gui.utils.NLS;

public final class RenameAction extends JNodeMenuAction<JNode> {
	private static final long serialVersionUID = -4680872086148463289L;

	private static final Logger LOG = LoggerFactory.getLogger(RenameAction.class);

	public RenameAction(CodeArea codeArea) {
		super(NLS.str("popup.rename"), codeArea);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (node == null) {
			LOG.info("node == null!");
			return;
		}
		RenameDialog renameDialog = new RenameDialog(codeArea, node);
		renameDialog.setVisible(true);
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
