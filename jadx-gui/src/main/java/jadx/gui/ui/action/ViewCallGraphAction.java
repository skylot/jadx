package jadx.gui.ui.action;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.CallGraphDialog;
import jadx.gui.utils.NLS;

public final class ViewCallGraphAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(ViewCallGraphAction.class);
	private static final long serialVersionUID = -11122327621269039L;

	public ViewCallGraphAction(CodeArea codeArea) {
		super(ActionModel.VIEW_CALL_GRAPH, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		try {

			JMethod methodNode;

			if (node instanceof JMethod) {
				methodNode = (JMethod) node;
			} else {
				throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
			}

			CallGraphDialog.open(getCodeArea().getMainWindow(), methodNode);
		} catch (Exception e) {
			LOG.error("Failed to view graph", e);
			JOptionPane.showMessageDialog(getCodeArea().getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod;
	}

}
