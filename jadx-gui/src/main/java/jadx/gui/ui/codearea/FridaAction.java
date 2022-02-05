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

import jadx.api.data.annotations.VarDeclareRef;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.MethodNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.utils.NLS;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FridaAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
	private static final long serialVersionUID = 4692546569977976384L;
	private final Map<String, Boolean> isInitial = new HashMap<>();

	public FridaAction(CodeArea codeArea) {

		super(NLS.str("popup.frida") + " (f)", codeArea);
		KeyStroke key = getKeyStroke(KeyEvent.VK_F, 0);
		codeArea.getInputMap().put(key, "trigger frida");
		codeArea.getActionMap().put("trigger frida", new AbstractAction() {
			@Override

			public void actionPerformed(ActionEvent e) {
				node = getNodeByOffset(codeArea.getWordStart(codeArea.getCaretPosition()));

				String fridaSnippet = generateFridaSnippet();

				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringSelection selection = new StringSelection(fridaSnippet);
				clipboard.setContents(selection, selection);
			}
		});
	}

	private String generateFridaSnippet() {
		if (node instanceof JMethod) {
			return generateMethodSnippet();

		} else if (node instanceof JClass) {
			return generateClassSnippet();
		}
		LOG.debug("cannot generate frida snippet from node");
		return "";

	}


	private String generateMethodSnippet() {
		JMethod jMth = (JMethod) node;
		assert jMth != null;
		MethodNode methodNode = jMth.getJavaMethod().getMethodNode();
		MethodInfo mi = methodNode.getMethodInfo();
		String methodName = mi.getName();
		if (methodName.equals("<init>") || methodName.equals("onCreate")) {
			methodName = "$init";
		}
		String rawClassName = methodNode.getParentClass().getRawName();
		String className = methodNode.getParentClass().getShortName();
		LOG.debug("node is jmethod");

		String functionUntilImplementation;
		if (!methodNode.getOverloads().isEmpty()) {
			List<ArgType> methodArgs = mi.getArgumentsTypes();
			String overloadStr = methodArgs.stream().map(this::parseArgType).collect(Collectors.joining(", "));
			functionUntilImplementation = String.format("%s.%s.overload(%s).implementation", className, methodName, overloadStr);
		} else {
			functionUntilImplementation = String.format("%s.%s.implementation", className, methodName);
		}

		String functionParametersString = Objects.requireNonNull(methodNode.getTopParentClass().getCode()).getAnnotations().entrySet().stream()
				.filter(e -> e.getKey().getLine() == jMth.getLine() && e.getValue() instanceof VarDeclareRef)
				.sorted(Comparator.comparingInt(e -> e.getKey().getPos()))
				.map(e -> ((VarDeclareRef) e.getValue()).getName())
				.collect(Collectors.joining(", "));


		String functionParameterAndBody = String.format(
				"%s = function(%s){\n\tconsole.log('%s is called')\n\tlet ret = this.%s(%s)\n\tconsole.log('%s ret value is ' + ret)\n\treturn ret\n}",
				functionUntilImplementation, functionParametersString, methodName, methodName, functionParametersString, methodName);

		String finalFridaCode;
		if (isInitial.getOrDefault(rawClassName, true)) {
			finalFridaCode = String.format("let %s = Java.use(\"%s\")\n%s", className, rawClassName, functionParameterAndBody);
			isInitial.put(rawClassName, false);
		} else {
			finalFridaCode = functionParameterAndBody;
		}
		LOG.debug("frida code : " + finalFridaCode);
		return finalFridaCode;
	}

	private String generateClassSnippet() {
		LOG.debug("node is jclass");
		JClass jc = (JClass) node;
		assert jc != null;
		String fullClassName = jc.getCls().getRawName();
		String className = jc.getCls().getName();
		String finalFridaCode = String.format("let %s = Java.use(\"%s\")", className, fullClassName);
		LOG.debug("frida code : " + finalFridaCode);
		isInitial.put(fullClassName, false);
		return finalFridaCode;
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
		generateFridaSnippet();
	}

	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
