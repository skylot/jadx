package jadx.gui.ui.codearea;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.data.annotations.VarDeclareRef;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FridaAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static final long serialVersionUID = 4692546569977976384L;
	private Map<String, Boolean> isInitial = new HashMap<>();
	private String methodName;

	public FridaAction(CodeArea codeArea) {

		super(NLS.str("popup.frida") + " (f)", codeArea);
		LOG.info("triggered meee");
		KeyStroke key = getKeyStroke(KeyEvent.VK_F, 0);
		codeArea.getInputMap().put(key, "trigger frida");
		codeArea.getActionMap().put("trigger frida", new AbstractAction() {
			@Override

			public void actionPerformed(ActionEvent e) {
				node = getNodeByOffset(codeArea.getWordStart(codeArea.getCaretPosition()));
				copyFridaCode();
			}
		});
	}

	private void copyFridaCode() {

		if (node != null) {
			if (node instanceof JMethod) {
				JMethod n = (JMethod) node;
				MethodNode methodNode = n.getJavaMethod().getMethodNode();
				MethodInfo mi = methodNode.getMethodInfo();
				methodName = mi.getName();
				if (methodName.equals("<init>") || methodName.equals("onCreate")) {
					methodName = "$init";
				}
				String fullClassName = methodNode.getParentClass().getFullName();
				String className = methodNode.getParentClass().getShortName();
				LOG.debug("node is jmethod");
				ClassNode tmp = methodNode.getParentClass();
				while (true) {
					if (!tmp.isTopClass()) {
						fullClassName = fullClassName.substring(0, fullClassName.lastIndexOf(".")) + "$"
								+ fullClassName.substring(fullClassName.lastIndexOf(".") + 1, fullClassName.length());
					} else {
						break;
					}
					tmp = tmp.getParentClass();
				}
				JMethod jMth = (JMethod) node;
				int mthLine = jMth.getLine();
				List<String> argNames = jMth.getRootClass().getCodeInfo().getAnnotations().entrySet().stream()
						.filter(e -> e.getKey().getLine() == mthLine && e.getValue() instanceof VarDeclareRef)
						.sorted(Comparator.comparingInt(e -> e.getKey().getPos()))
						.map(e -> ((VarDeclareRef) e.getValue()).getName())
						.collect(Collectors.toList());

				StringBuilder functionParameters = new StringBuilder();
				for (String argName : argNames) {
					functionParameters.append(argName + ", ");
				}
				if (functionParameters.toString().length() > 2) {
					functionParameters.setLength(functionParameters.length() - 2);
				}

				List<MethodNode> methods = methodNode.getParentClass().getMethods();
				List<MethodNode> filteredmethod = methods.stream().filter(m -> m.getName().equals(methodName)).collect(Collectors.toList());
				StringBuilder sb = new StringBuilder();
				String overloadStr = "";
				if (filteredmethod.size() > 1) {
					List<ArgType> methodArgs = mi.getArgumentsTypes();
					for (ArgType argType : methodArgs) {
						sb.append("'" + parseArgType(argType) + "', ");
					}
					if (sb.length() > 2) {
						sb.setLength(sb.length() - 2);
					}
					overloadStr = sb.toString();

				}
				String functionUntilImplementation = "";
				if (!overloadStr.equals("")) {
					functionUntilImplementation = String.format("%s.%s.overload(%s).implementation", className, methodName, overloadStr);
				} else {
					functionUntilImplementation = String.format("%s.%s.implementation", className, methodName);
				}
				String functionParameterAndBody = "";
				String functionParametersString = functionParameters.toString();
				if (!functionParametersString.equals("")) {
					functionParameterAndBody = String.format(
							"%s = function(%s){\n\tconsole.log('%s is called')\n\tlet ret = this.%s(%s)\n\tconsole.log('%s ret value is ' + ret)\n\treturn ret\n}",
							functionUntilImplementation, functionParametersString, methodName, methodName, functionParametersString,
							methodName);
				} else {
					functionParameterAndBody = String.format(
							"%s = function(){\n\tconsole.log('%s is called')\n\tlet ret = this.%s()\n\tconsole.log('%s ret value is ' + ret)\n\treturn ret\n}",
							functionUntilImplementation, methodName, methodName, methodName);
				}
				String finalFridaCode = "";
				if (isInitial.getOrDefault(fullClassName, true)) {
					finalFridaCode = String.format("let %s = Java.use(\"%s\")\n%s", className, fullClassName, functionParameterAndBody);
					isInitial.put(fullClassName, false);
				} else {
					finalFridaCode = functionParameterAndBody;
				}
				LOG.debug("frida code : " + finalFridaCode);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(finalFridaCode);
				clipboard.setContents(selection, selection);

			} else if (node instanceof JClass) {
				LOG.debug("node is jclass");
				JClass jc = (JClass) node;
				String fullClassName = jc.getCls().getClassNode().getClassInfo().getFullName();
				String className = jc.getCls().getClassNode().getClassInfo().getShortName();
				ClassNode tmp = jc.getCls().getClassNode();
				while (true) {
					if (!tmp.isTopClass()) {
						fullClassName = fullClassName.substring(0, fullClassName.lastIndexOf(".")) + "$"
								+ fullClassName.substring(fullClassName.lastIndexOf(".") + 1, fullClassName.length());
					} else {
						break;
					}
					tmp = tmp.getParentClass();
				}
				String finalFridaCode = String.format("let %s = Java.use(\"%s\")", className, fullClassName);
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(finalFridaCode);
				clipboard.setContents(selection, selection);
				LOG.debug("frida code : " + finalFridaCode);
				isInitial.put(fullClassName, false);
			} else {
				LOG.debug("node is something else");
			}

		}
	}

	private String parseArgType(ArgType x) {
		StringBuilder parsedArgType = new StringBuilder();
		if (x.isArray()) {
			parsedArgType.append(x.getPrimitiveType().getShortName());
			parsedArgType.append(x.getArrayElement().getPrimitiveType().getShortName());
			if (!x.getArrayElement().isPrimitive()) {
				parsedArgType.append(x.getArrayElement().toString() + ";");
			}

		} else {
			parsedArgType.append(x.toString());
		}
		return parsedArgType.toString();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		node = codeArea.getNodeUnderCaret();
		copyFridaCode();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
