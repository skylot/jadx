package jadx.gui.ui.codearea;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.data.annotations.VarDeclareRef;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public class XposedAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(XposedAction.class);
	private static final long serialVersionUID = 4692546569977976384L;
	private final Map<String, Boolean> isInitial = new HashMap<>();

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

	private void copyXposedSnippet() {
		try {
			String XposedSnippet = generateXposedSnippet();
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(XposedSnippet);
			clipboard.setContents(selection, selection);
		} catch (Exception e) {
			LOG.error("Failed to generate Xposed code snippet", e);
			JOptionPane.showMessageDialog(codeArea.getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private String generateXposedSnippet() {
		if (node instanceof JMethod) {
			LOG.debug("node is jmethod");
			return generateMethodSnippet((JMethod) node);
		} else if (node instanceof JClass) {
			LOG.debug("node is jclass");
			return generateClassSnippet((JClass) node);
		}
		throw new JadxRuntimeException("Unsupported node type: " + node.getClass());
	}

	private String generateMethodSnippet(JMethod jMth) {
		JavaMethod javaMethod = jMth.getJavaMethod();
		MethodInfo methodInfo = javaMethod.getMethodNode().getMethodInfo();
		String methodName = methodInfo.getName();
		methodName=String.format("\"%s\",",methodName);
		String xposed_method="findAndHookMethod";
		if (methodName.indexOf("<init>")!=-1) {
			xposed_method="findAndHookConstructor";
			methodName="";
		}
		String rawClassName = javaMethod.getDeclaringClass().getRawName();
		String shortClassName = javaMethod.getDeclaringClass().getName();
		String xposedformatnoparm="XposedHelpers.%s(\"%s\", classLoader, %s new XC_MethodHook() {\n" +
				"    @Override\n" +
				"    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n" +
				"        super.beforeHookedMethod(param);\n" +
				"    }\n" +
				"    @Override\n" +
				"    protected void afterHookedMethod(MethodHookParam param) throws Throwable {\n" +
				"        super.afterHookedMethod(param);\n" +
				"    }";
		String xposedformatparm="XposedHelpers.%s(\"%s\", classLoader, %s %s, new XC_MethodHook() {\n" +
				"    @Override\n" +
				"    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {\n" +
				"        super.beforeHookedMethod(param);\n" +
				"    }\n" +
				"    @Override\n" +
				"    protected void afterHookedMethod(MethodHookParam param) throws Throwable {\n" +
				"        super.afterHookedMethod(param);\n" +
				"    }\n" +
				"\t});";
		String functionParametersString =
				Objects.requireNonNull(javaMethod.getTopParentClass().getCodeInfo()).getAnnotations().entrySet().stream()
						.filter(e -> e.getKey().getLine() == jMth.getLine() && e.getValue() instanceof VarDeclareRef)
						.sorted(Comparator.comparingInt(e -> e.getKey().getPos()))
						.map(e -> ((VarDeclareRef) e.getValue()).getType().toString())
						.collect(Collectors.joining(", "));
		String finalxposed;
		if(functionParametersString!=null)
		{
			String[] parmnames=functionParametersString.split(",");
			String finalparms="";
			for(int i=0;i<parmnames.length;i++)
			{
				parmnames[i]=parmnames[i]+".class";
				finalparms+=parmnames[i]+",";
			}
			finalparms=finalparms.substring(0,finalparms.length()-1);
			 finalxposed=String.format(xposedformatparm,xposed_method,rawClassName,methodName,finalparms);
		}else {
			finalxposed=String.format(xposedformatnoparm,xposed_method,rawClassName,methodName);
		}
		String finalXposedCode=finalxposed;
		LOG.debug("Xposed code : {}", finalXposedCode);
		return finalXposedCode;
	}

	private String generateClassSnippet(JClass jc) {
		JavaClass javaClass = jc.getCls();
		String rawClassName = javaClass.getRawName();
		String shortClassName = javaClass.getName();
		String finalXposedCode = String.format("ClassLoader classLoader=lpparam.classLoader;\n" +
												"Class %sClass=classLoader.loadClass(\"%s\");", shortClassName, rawClassName);
		LOG.debug("Xposed code : {}", finalXposedCode);
		isInitial.put(rawClassName, false);
		return finalXposedCode;
	}



	public Boolean isOverloaded(MethodNode methodNode) {
		ClassNode parentClass = methodNode.getParentClass();
		List<MethodNode> methods = parentClass.getMethods();
		return methods.stream()
				.anyMatch(m -> m.getName().equals(methodNode.getName())
						&& !Objects.equals(methodNode.getMethodInfo().getShortId(), m.getMethodInfo().getShortId()));
	}

	private String parseArgType(ArgType x) {
		StringBuilder parsedArgType = new StringBuilder("'");
		if (x.isArray()) {
			parsedArgType.append(x.getPrimitiveType().getShortName());
			parsedArgType.append(x.getArrayElement().getPrimitiveType().getShortName());
			if (!x.getArrayElement().isPrimitive()) {
				parsedArgType.append(x.getArrayElement().toString()).append(";");
			}

		} else {
			parsedArgType.append(x);
		}
		return parsedArgType.append("'").toString();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		node = codeArea.getNodeUnderCaret();
		copyXposedSnippet();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
