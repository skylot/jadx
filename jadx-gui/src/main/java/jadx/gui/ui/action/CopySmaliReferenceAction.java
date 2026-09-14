package jadx.gui.ui.action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.TypeGen;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.UiUtils;

/**
 * Copy original (not renamed) class/method/field reference in smali format,
 * e.g. {@code Lcom/example/Cls;->method(I[Ljava/lang/String;)V}
 */
public final class CopySmaliReferenceAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(CopySmaliReferenceAction.class);
	private static final long serialVersionUID = 3504906924823485163L;

	public CopySmaliReferenceAction(CodeArea codeArea) {
		super(ActionModel.COPY_SMALI_REFERENCE, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		JavaNode javaNode = node.getJavaNode();
		String ref;
		if (javaNode instanceof JavaClass) {
			ref = getSmaliReference(((JavaClass) javaNode).getClassNode().getClassInfo());
		} else if (javaNode instanceof JavaMethod) {
			ref = getSmaliReference(((JavaMethod) javaNode).getMethodNode().getMethodInfo());
		} else if (javaNode instanceof JavaField) {
			ref = getSmaliReference(((JavaField) javaNode).getFieldNode().getFieldInfo());
		} else {
			LOG.warn("Copy smali reference not supported for node type: {}", node.getClass());
			return;
		}
		UiUtils.copyToClipboard(ref);
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod || node instanceof JClass || node instanceof JField;
	}

	static String getSmaliReference(ClassInfo cls) {
		return TypeGen.signature(cls.getType());
	}

	static String getSmaliReference(MethodInfo mth) {
		return getSmaliReference(mth.getDeclClass()) + "->" + mth.getShortId();
	}

	static String getSmaliReference(FieldInfo fld) {
		return getSmaliReference(fld.getDeclClass()) + "->" + fld.getName() + ':' + TypeGen.signature(fld.getType());
	}
}
