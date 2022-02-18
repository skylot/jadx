package jadx.gui.ui.codearea;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;

import org.jetbrains.annotations.Nullable;
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

import static javax.swing.KeyStroke.getKeyStroke;

public class XposedAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(XposedAction.class);
	private static final long serialVersionUID = 2641585141624592578L;

	public XposedAction(CodeArea codeArea) {
		super(NLS.str("popup.xposed") + " (y)", codeArea);
		KeyStroke key = getKeyStroke(KeyEvent.VK_Y, 0);
		codeArea.getInputMap().put(key, "trigger xposed");
		codeArea.getActionMap().put("trigger xposed", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				node = getNodeByOffset(codeArea.getWordStart(codeArea.getCaretPosition()));
				copyXposedSnippet();
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		node = codeArea.getNodeUnderCaret();
		copyXposedSnippet();
	}

	private void copyXposedSnippet() {
		try {
			String xposedSnippet = generateXposedSnippet();
			LOG.info("Xposed snippet:\n{}", xposedSnippet);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(xposedSnippet);
			clipboard.setContents(selection, selection);
		} catch (Exception e) {
			LOG.error("Failed to generate Xposed code snippet", e);
			JOptionPane.showMessageDialog(codeArea.getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private String generateXposedSnippet() {
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

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
