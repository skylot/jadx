package jadx.gui.ui.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.UiUtils;

public final class CopyReferenceAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(CopyReferenceAction.class);
	private static final long serialVersionUID = -8816072267744391424L;

	public CopyReferenceAction(CodeArea codeArea) {
		super(ActionModel.COPY_REFERENCE, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		JavaNode javaNode = node.getJavaNode();
		String ref;
		if (javaNode instanceof JavaClass) {
			ref = javaNode.getFullName();
		} else if (javaNode instanceof JavaMethod) {
			ref = ((JavaMethod) javaNode).getDeclaringClass().getFullName() + '.' + javaNode.getName();
		} else if (javaNode instanceof JavaField) {
			ref = ((JavaField) javaNode).getDeclaringClass().getFullName() + '.' + javaNode.getName();
		} else {
			LOG.warn("Copy reference not supported for node type: {}", node.getClass());
			return;
		}
		UiUtils.copyToClipboard(ref);
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod || node instanceof JClass || node instanceof JField;
	}
}
