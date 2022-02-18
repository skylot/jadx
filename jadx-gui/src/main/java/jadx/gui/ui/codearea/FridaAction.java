package jadx.gui.ui.codearea;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.stream.Collectors;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;

import jadx.core.codegen.TypeGen;
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

public final class FridaAction extends JNodeMenuAction<JNode> {
	private static final Logger LOG = LoggerFactory.getLogger(FridaAction.class);
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
				copyFridaSnippet();
			}
		});
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		node = codeArea.getNodeUnderCaret();
		copyFridaSnippet();
	}

	private void copyFridaSnippet() {
		try {
			String fridaSnippet = generateFridaSnippet();
			LOG.info("Frida snippet:\n{}", fridaSnippet);
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			StringSelection selection = new StringSelection(fridaSnippet);
			clipboard.setContents(selection, selection);
		} catch (Exception e) {
			LOG.error("Failed to generate Frida code snippet", e);
			JOptionPane.showMessageDialog(codeArea.getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private String generateFridaSnippet() {
		if (node instanceof JMethod) {
			return generateMethodSnippet((JMethod) node);
		} else if (node instanceof JClass) {
			return generateClassSnippet((JClass) node);
		} else if (node instanceof JField) {
			return generateFieldSnippet((JField) node);
		}
		throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
	}

	private String generateMethodSnippet(JMethod jMth) {
		JavaMethod javaMethod = jMth.getJavaMethod();
		MethodInfo methodInfo = javaMethod.getMethodNode().getMethodInfo();
		String methodName = methodInfo.getName();
		if (methodInfo.isConstructor()) {
			methodName = "$init";
		}
		String rawClassName = javaMethod.getDeclaringClass().getRawName();
		String shortClassName = javaMethod.getDeclaringClass().getName();

		String functionUntilImplementation;
		if (isOverloaded(javaMethod.getMethodNode())) {
			List<ArgType> methodArgs = methodInfo.getArgumentsTypes();
			String overloadStr = methodArgs.stream().map(this::parseArgType).collect(Collectors.joining(", "));
			functionUntilImplementation = String.format("%s.%s.overload(%s).implementation", shortClassName, methodName, overloadStr);
		} else {
			functionUntilImplementation = String.format("%s.%s.implementation", shortClassName, methodName);
		}

		String functionParametersString =
				Objects.requireNonNull(javaMethod.getTopParentClass().getCodeInfo()).getAnnotations().entrySet().stream()
						.filter(e -> e.getKey().getLine() == jMth.getLine() && e.getValue() instanceof VarDeclareRef)
						.sorted(Comparator.comparingInt(e -> e.getKey().getPos()))
						.map(e -> ((VarDeclareRef) e.getValue()).getName())
						.collect(Collectors.joining(", "));

		String functionParameterAndBody = String.format(
				"%s = function(%s){\n" +
						"    console.log('%s is called');\n" +
						"    let ret = this.%s(%s);\n" +
						"    console.log('%s ret value is ' + ret);\n" +
						"    return ret;\n" +
						"};",
				functionUntilImplementation, functionParametersString, methodName, methodName, functionParametersString, methodName);

		String finalFridaCode;
		if (isInitial.getOrDefault(rawClassName, true)) {
			String classSnippet = generateClassSnippet(jMth.getJParent());
			finalFridaCode = classSnippet + "\n" + functionParameterAndBody;
		} else {
			finalFridaCode = functionParameterAndBody;
		}
		return finalFridaCode;
	}

	private String generateClassSnippet(JClass jc) {
		JavaClass javaClass = jc.getCls();
		String rawClassName = javaClass.getRawName();
		String shortClassName = javaClass.getName();
		String finalFridaCode = String.format("let %s = Java.use(\"%s\");", shortClassName, rawClassName);
		isInitial.put(rawClassName, false);
		return finalFridaCode;
	}

	private String generateFieldSnippet(JField jf) {
		JavaField javaField = jf.getJavaField();
		String rawFieldName = javaField.getRawName();
		String fieldName = javaField.getName();

		List<MethodNode> methodNodes = javaField.getFieldNode().getParentClass().getMethods();
		for (MethodNode methodNode : methodNodes) {
			if (methodNode.getName().equals(rawFieldName)) {
				rawFieldName = "_" + rawFieldName;
				break;
			}
		}

		JClass jc = jf.getRootClass();
		String classSnippet = generateClassSnippet(jc);
		return String.format("%s\n%s = %s.%s.value;", classSnippet, fieldName, jc.getName(), rawFieldName);
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
			parsedArgType.append(TypeGen.signature(x).replace("/", "."));
		} else {
			parsedArgType.append(x);
		}
		return parsedArgType.append("'").toString();
	}


	@Nullable
	@Override
	public JNode getNodeByOffset(int offset) {
		return codeArea.getJNodeAtOffset(offset);
	}
}
