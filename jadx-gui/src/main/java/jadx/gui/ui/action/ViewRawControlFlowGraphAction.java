package jadx.gui.ui.action;

import java.io.File;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.DotGraphUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.ControlFlowGraphDialog;
import jadx.gui.utils.NLS;

public final class ViewRawControlFlowGraphAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(ViewRawControlFlowGraphAction.class);
	private static final long serialVersionUID = -535703386523657L;

	public ViewRawControlFlowGraphAction(CodeArea codeArea) {
		super(ActionModel.VIEW_RAW_CONTROL_FLOW_GRAPH, codeArea);
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

			ControlFlowGraphDialog.open(getCodeArea().getMainWindow(), methodNode, false, true);
		} catch (Exception e) {
			LOG.error("Failed to view graph", e);
			JOptionPane.showMessageDialog(getCodeArea().getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		if (!(node instanceof JMethod)) {
			return false;
		}
		MethodNode mth = ((JMethod) node).getJavaMethod().getMethodNode();
		File file = new DotGraphUtils(false, true).getFullFile(mth);

		return file.exists() && !file.isDirectory();
	}

}
