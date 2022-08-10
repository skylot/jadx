package jadx.gui.ui.codearea;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.apache.commons.text.StringEscapeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.metadata.annotations.VarNode;
import jadx.core.codegen.TypeGen;
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
import jadx.gui.utils.UiUtils;

import static javax.swing.KeyStroke.getKeyStroke;

public final class FridaAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(FridaAction.class);
	private static final long serialVersionUID = -3084073927621269039L;

	public FridaAction(CodeArea codeArea) {
		super(NLS.str("popup.frida") + " (f)", codeArea);
		addKeyBinding(getKeyStroke(KeyEvent.VK_F, 0), "trigger frida");
	}

	@Override
	public void runAction(JNode node) {
		try {
			String fridaSnippet = generateFridaSnippet(node);
			LOG.info("Frida snippet:\n{}", fridaSnippet);
			UiUtils.copyToClipboard(fridaSnippet);
		} catch (Exception e) {
			LOG.error("Failed to generate Frida code snippet", e);
			JOptionPane.showMessageDialog(getCodeArea().getMainWindow(), e.getLocalizedMessage(), NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	@Override
	public boolean isActionEnabled(JNode node) {
		return node instanceof JMethod || node instanceof JClass || node instanceof JField;
	}

	private String generateFridaSnippet(JNode node) {
		if (node instanceof JMethod) {
			return generateMethodSnippet((JMethod) node);
		}
		if (node instanceof JClass) {
			return generateClassSnippet((JClass) node);
		}
		if (node instanceof JField) {
			return generateFieldSnippet((JField) node);
		}
		throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
	}

	private String generateMethodSnippet(JMethod jMth) {
		JavaMethod javaMethod = jMth.getJavaMethod();
		MethodInfo methodInfo = javaMethod.getMethodNode().getMethodInfo();
		String methodName = StringEscapeUtils.escapeEcmaScript(methodInfo.getName());
		String callMethodName = methodName;

		if (methodInfo.isConstructor()) {
			methodName = "$init";
			callMethodName = "$new";
		}
		String shortClassName = javaMethod.getDeclaringClass().getName();

		String functionUntilImplementation;
		if (isOverloaded(javaMethod.getMethodNode())) {
			List<ArgType> methodArgs = methodInfo.getArgumentsTypes();
			String overloadStr = methodArgs.stream().map(this::parseArgType).collect(Collectors.joining(", "));
			functionUntilImplementation = String.format("%s[\"%s\"].overload(%s).implementation", shortClassName, methodName, overloadStr);
		} else {
			functionUntilImplementation = String.format("%s[\"%s\"].implementation", shortClassName, methodName);
		}

		List<String> methodArgNames = new ArrayList<>();
		for (VarNode arg : javaMethod.getMethodNode().collectArgsWithoutLoading()) {
			methodArgNames.add(arg.getName());
		}

		String functionParametersString = String.join(", ", methodArgNames);
		String logParametersString =
				methodArgNames.stream().map(e -> String.format("'%s: ' + %s", e, e)).collect(Collectors.joining(" + ', ' + "));
		if (logParametersString.length() > 0) {
			logParametersString = " + ', ' + " + logParametersString;
		}
		String functionParameterAndBody = String.format(
				"%s = function (%s) {\n"
						+ "    console.log('%s is called'%s);\n"
						+ "    let ret = this.%s(%s);\n"
						+ "    console.log('%s ret value is ' + ret);\n"
						+ "    return ret;\n"
						+ "};",
				functionUntilImplementation, functionParametersString, methodName, logParametersString, callMethodName,
				functionParametersString, methodName);

		return generateClassSnippet(jMth.getJParent()) + "\n" + functionParameterAndBody;
	}

	private String generateClassSnippet(JClass jc) {
		JavaClass javaClass = jc.getCls();
		String rawClassName = StringEscapeUtils.escapeEcmaScript(javaClass.getRawName());
		String shortClassName = javaClass.getName();
		return String.format("let %s = Java.use(\"%s\");", shortClassName, rawClassName);
	}

	private String generateFieldSnippet(JField jf) {
		JavaField javaField = jf.getJavaField();
		String rawFieldName = StringEscapeUtils.escapeEcmaScript(javaField.getRawName());
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
}
