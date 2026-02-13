package jadx.gui.ui.action;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.ClassInheritanceGraphDialog;
import jadx.gui.utils.NLS;

public final class ViewClassInheritanceGraphAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(ViewClassInheritanceGraphAction.class);
	private static final long serialVersionUID = -331826691076655264L;

	public ViewClassInheritanceGraphAction(CodeArea codeArea) {
		super(ActionModel.VIEW_CLASS_INHERITANCE_GRAPH, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		try {

			JClass classNode;

			if (node instanceof JMethod) {
				classNode = node.getJParent();
			} else if (node instanceof JField) {
				classNode = node.getJParent();
			} else if (node instanceof JClass) {
				classNode = (JClass) node;
			} else {
				throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
			}

			ClassInheritanceGraphDialog.open(getCodeArea().getMainWindow(), classNode);
		} catch (Exception e) {
			LOG.error("Failed to view graph", e);
			JOptionPane.showMessageDialog(getCodeArea().getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod || node instanceof JClass || node instanceof JField;
	}

}
