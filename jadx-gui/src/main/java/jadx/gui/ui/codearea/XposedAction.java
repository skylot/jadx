package jadx.gui.ui.codearea;

import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

import static javax.swing.KeyStroke.getKeyStroke;

public class XposedAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(XposedAction.class);
	private static final long serialVersionUID = 2641585141624592578L;

	public XposedAction(CodeArea codeArea) {
		super(NLS.str("popup.xposed") + " (y)", codeArea);
		addKeyBinding(getKeyStroke(KeyEvent.VK_Y, 0), "trigger xposed");
	}

	@Override
	public void runAction(JNode node) {
		try {
			String xposedSnippet = generateXposedSnippet(node);
			LOG.info("Xposed snippet:\n{}", xposedSnippet);
			UiUtils.copyToClipboard(xposedSnippet);
		} catch (Exception e) {
			LOG.error("Failed to generate Xposed code snippet", e);
			JOptionPane.showMessageDialog(getCodeArea().getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod || node instanceof JClass;
	}

	private String generateXposedSnippet(JNode node) {
		if (node instanceof JMethod) {
			return generateMethodSnippet((JMethod) node);
		}
		if (node instanceof JClass) {
			return generateClassSnippet((JClass) node);
		}
		throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
	}

	private String generateMethodSnippet(JMethod jMth) {
		JavaMethod javaMethod = jMth.getJavaMethod();
		MethodNode mth = javaMethod.getMethodNode();
		String methodName;
		String xposedMethod;
		if (mth.isConstructor()) {
			xposedMethod = "findAndHookConstructor";
			methodName = "";
		} else {
			xposedMethod = "findAndHookMethod";
			methodName = "\"" + mth.getMethodInfo().getName() + "\", ";
		}
		String rawClassName = javaMethod.getDeclaringClass().getRawName();
		String xposedFormatStr = "XposedHelpers.%s(\"%s\", classLoader, %snew XC_MethodHook() {\n"
				+ "    @Override\n"
				+ "    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n"
				+ "        super.beforeHookedMethod(param);\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    protected void afterHookedMethod(MethodHookParam param) throws Throwable {\n"
				+ "        super.afterHookedMethod(param);\n"
				+ "    }\n"
				+ "});";

		List<ArgType> mthArgs = mth.getArgTypes();
		if (mthArgs.isEmpty()) {
			return String.format(xposedFormatStr, xposedMethod, rawClassName, methodName);
		}
		String params = mthArgs.stream().map(type -> type + ".class, ").collect(Collectors.joining());
		return String.format(xposedFormatStr, xposedMethod, rawClassName, methodName + params);
	}

	private String generateClassSnippet(JClass jc) {
		JavaClass javaClass = jc.getCls();
		String rawClassName = javaClass.getRawName();
		String shortClassName = javaClass.getName();
		return String.format("ClassLoader classLoader=lpparam.classLoader;\n"
				+ "Class %sClass=classLoader.loadClass(\"%s\");",
				shortClassName, rawClassName);
	}
}
