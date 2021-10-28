package jadx.gui.ui.codearea;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.KeyStroke;
import javax.swing.event.PopupMenuEvent;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.treemodel.JNode;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static java.awt.event.KeyEvent.VK_N;
import static javax.swing.KeyStroke.getKeyStroke;

public final class RenameAction extends JNodeMenuAction<JNode> {
	private static final long serialVersionUID = -4680872086148463289L;

	private static final Logger LOG = LoggerFactory.getLogger(RenameAction.class);

	public RenameAction(CodeArea codeArea) {
		super(NLS.str("popup.rename") + " (n)", codeArea);
		KeyStroke key = getKeyStroke(VK_N, 0);
		String renameActionId = "trigger rename";
		codeArea.getInputMap().put(key, renameActionId);
		codeArea.getActionMap().put(renameActionId, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node = codeArea.getNodeUnderCaret();
				showRenameDialog();
			}
		});
	}

	private void showRenameDialog() {
		if (node == null) {
			LOG.info("node == null!");
			UiUtils.showMessageBox(codeArea.getMainWindow(), NLS.str("msg.rename_node_disabled"));
			return;
		}
		if (!node.canRename()) {
			UiUtils.showMessageBox(codeArea.getMainWindow(),
					NLS.str("msg.rename_node_failed", node.getJavaNode().getFullName()));
			LOG.warn("Can't rename node: {}", node);
			return;
		}
		RenameDialog.rename(codeArea.getMainWindow(), codeArea.getNode(), node);
		node = null;
	}

	@Override
	public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
		super.popupMenuWillBecomeVisible(e);
		setEnabled(node != null && node.canRename());
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		showRenameDialog();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
