package jadx.gui.ui.action;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

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
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.dialog.MethodsDialog;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;

public final class FridaAction extends JNodeAction {
	private static final Logger LOG = LoggerFactory.getLogger(FridaAction.class);
	private static final long serialVersionUID = -3084073927621269039L;

	public FridaAction(CodeArea codeArea) {
		super(ActionModel.FRIDA_COPY, codeArea);
	}

	@Override
	public void runAction(JNode node) {
		try {
			generateFridaSnippet(node);
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

	private void generateFridaSnippet(JNode node) {
		String fridaSnippet;
		if (node instanceof JMethod) {
			fridaSnippet = generateMethodSnippet((JMethod) node);
			copySnipped(fridaSnippet);
		} else if (node instanceof JField) {
			fridaSnippet = generateFieldSnippet((JField) node);
			copySnipped(fridaSnippet);
		} else if (node instanceof JClass) {
			SwingUtilities.invokeLater(() -> showMethodSelectionDialog((JClass) node));
		} else {
			throw new JadxRuntimeException("Unsupported node type: " + (node != null ? node.getClass() : "null"));
		}

	}

	private void copySnipped(String fridaSnippet) {
		if (!StringUtils.isEmpty(fridaSnippet)) {
			LOG.info("Frida snippet:\n{}", fridaSnippet);
			UiUtils.copyToClipboard(fridaSnippet);
		}
	}

	private String generateMethodSnippet(JMethod jMth) {
		String classSnippet = generateClassSnippet(jMth.getJParent());
		String methodSnippet = getMethodSnippet(jMth.getJavaMethod(), jMth.getJParent());
		return String.format("%s\n%s", classSnippet, methodSnippet);
	}

	private String generateMethodSnippet(JavaMethod javaMethod, JClass jc) {
		return getMethodSnippet(javaMethod, jc);
	}

	private String getMethodSnippet(JavaMethod javaMethod, JClass jc) {
		MethodNode mth = javaMethod.getMethodNode();
		MethodInfo methodInfo = mth.getMethodInfo();
		String methodName;
		String newMethodName;
		if (methodInfo.isConstructor()) {
			methodName = "$init";
			newMethodName = methodName;
		} else {
			methodName = StringEscapeUtils.escapeEcmaScript(methodInfo.getName());
			newMethodName = StringEscapeUtils.escapeEcmaScript(methodInfo.getAlias());
		}
		String overload;
		if (isOverloaded(mth)) {
			String overloadArgs = methodInfo.getArgumentsTypes().stream()
					.map(this::parseArgType).collect(Collectors.joining(", "));
			overload = ".overload(" + overloadArgs + ")";
		} else {
			overload = "";
		}
		List<String> argNames = mth.collectArgNodes().stream()
				.map(VarNode::getName).collect(Collectors.toList());
		String args = String.join(", ", argNames);
		String logArgs;
		if (argNames.isEmpty()) {
			logArgs = "";
		} else {
			logArgs = ": " + argNames.stream().map(arg -> arg + "=${" + arg + "}").collect(Collectors.joining(", "));
		}
		String shortClassName = mth.getParentClass().getAlias();
		if (methodInfo.isConstructor() || methodInfo.getReturnType() == ArgType.VOID) {
			// no return value
			return shortClassName + "[\"" + methodName + "\"]" + overload + ".implementation = function (" + args + ") {\n"
					+ "    console.log(`" + shortClassName + "." + newMethodName + " is called" + logArgs + "`);\n"
					+ "    this[\"" + methodName + "\"](" + args + ");\n"
					+ "};";
		}
		return shortClassName + "[\"" + methodName + "\"]" + overload + ".implementation = function (" + args + ") {\n"
				+ "    console.log(`" + shortClassName + "." + newMethodName + " is called" + logArgs + "`);\n"
				+ "    let result = this[\"" + methodName + "\"](" + args + ");\n"
				+ "    console.log(`" + shortClassName + "." + newMethodName + " result=${result}`);\n"
				+ "    return result;\n"
				+ "};";
	}

	private String generateClassSnippet(JClass jc) {
		JavaClass javaClass = jc.getCls();
		String rawClassName = StringEscapeUtils.escapeEcmaScript(javaClass.getRawName());
		String shortClassName = javaClass.getName();
		return String.format("let %s = Java.use(\"%s\");", shortClassName, rawClassName);
	}

	private void showMethodSelectionDialog(JClass jc) {
		JavaClass javaClass = jc.getCls();
		new MethodsDialog(getCodeArea().getMainWindow(), javaClass.getMethods(), (result) -> {
			String fridaSnippet = generateClassAllMethodSnippet(jc, result);
			copySnipped(fridaSnippet);
		});
	}

	private String generateClassAllMethodSnippet(JClass jc, List<JavaMethod> methodList) {
		StringBuilder result = new StringBuilder();
		String classSnippet = generateClassSnippet(jc);
		result.append(classSnippet).append("\n");
		for (JavaMethod javaMethod : methodList) {
			result.append(generateMethodSnippet(javaMethod, jc)).append("\n");
		}
		return result.toString();
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
		return methodNode.getParentClass().getMethods().stream()
				.anyMatch(m -> m.getName().equals(methodNode.getName())
						&& !Objects.equals(methodNode.getMethodInfo().getShortId(), m.getMethodInfo().getShortId()));
	}

	private String parseArgType(ArgType x) {
		String typeStr;
		if (x.isArray()) {
			typeStr = TypeGen.signature(x).replace("/", ".");
		} else {
			typeStr = x.toString();
		}
		return "'" + typeStr + "'";
	}
}
