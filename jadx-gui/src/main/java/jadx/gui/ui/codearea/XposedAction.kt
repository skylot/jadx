package jadx.gui.ui.codearea

import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.instructions.args.PrimitiveType
import jadx.core.utils.exceptions.JadxRuntimeException
import jadx.gui.settings.XposedCodegenLanguage
import jadx.gui.treemodel.JClass
import jadx.gui.treemodel.JField
import jadx.gui.treemodel.JMethod
import jadx.gui.treemodel.JNode
import jadx.gui.ui.action.ActionModel
import jadx.gui.utils.NLS
import jadx.gui.utils.UiUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.swing.JOptionPane

class XposedAction(codeArea: CodeArea) : JNodeAction(ActionModel.XPOSED_COPY, codeArea) {
	override fun runAction(node: JNode) {
		try {
			val xposedSnippet = generateXposedSnippet(node)
			LOG.info("Xposed snippet:\n{}", xposedSnippet)
			UiUtils.copyToClipboard(xposedSnippet)
		} catch (e: Exception) {
			LOG.error("Failed to generate Xposed code snippet", e)
			JOptionPane.showMessageDialog(
				getCodeArea().mainWindow,
				e.localizedMessage,
				NLS.str("error_dialog.title"),
				JOptionPane.ERROR_MESSAGE,
			)
		}
	}

	override fun isActionEnabled(node: JNode?): Boolean {
		return node is JMethod || node is JClass || node is JField
	}

	private fun generateXposedSnippet(node: JNode): String {
		return when (node) {
			is JMethod -> generateMethodSnippet(node)
			is JClass -> generateClassSnippet(node)
			is JField -> generateFieldSnippet(node)
			else -> throw JadxRuntimeException("Unsupported node type: " + node.javaClass)
		}
	}

	private fun generateMethodSnippet(jMethod: JMethod): String {
		val javaMethod = jMethod.javaMethod
		val methodNode = javaMethod.methodNode
		val methodInfo = methodNode.methodInfo

		val xposedMethod: String
		var args = methodInfo.argumentsTypes.map(::fixTypeContent)
		val rawClassName = javaMethod.declaringClass.rawName

		if (methodNode.isConstructor) {
			xposedMethod = "findAndHookConstructor"
		} else {
			xposedMethod = "findAndHookMethod"
			args = listOf("\"${methodInfo.name}\"") + args
		}

		val template = when (language) {
			XposedCodegenLanguage.JAVA ->
				"""XposedHelpers.%s("%s", classLoader, %s, new XC_MethodHook() {
    @Override
    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
        super.beforeHookedMethod(param);
    }
    @Override
    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
        super.afterHookedMethod(param);
    }
});"""
			XposedCodegenLanguage.KOTLIN ->
				"""XposedHelpers.%s("%s", classLoader, %s, object : XC_MethodHook() {
    override fun beforeHookedMethod(param: MethodHookParam) {
        super.beforeHookedMethod(param)
    }

    override fun afterHookedMethod(param: MethodHookParam) {
        super.afterHookedMethod(param)
    }
})"""
		}

		return String.format(template, xposedMethod, rawClassName, args.joinToString(", "))
	}

	private fun fixTypeContent(type: ArgType): String {
		return when {
			type.isGeneric -> "\"${type.`object`}\""
			type.isGenericType && type.isObject && type.isTypeKnown -> "java.lang.Object"
			type.isPrimitive -> when (language) {
				XposedCodegenLanguage.JAVA -> "$type.class"
				XposedCodegenLanguage.KOTLIN -> when (type.primitiveType) {
					PrimitiveType.BOOLEAN -> "Boolean::class.javaPrimitiveType"
					PrimitiveType.CHAR -> "Char::class.javaPrimitiveType"
					PrimitiveType.BYTE -> "Byte::class.javaPrimitiveType"
					PrimitiveType.SHORT -> "Short::class.javaPrimitiveType"
					PrimitiveType.INT -> "Int::class.javaPrimitiveType"
					PrimitiveType.FLOAT -> "Float::class.javaPrimitiveType"
					PrimitiveType.LONG -> "Long::class.javaPrimitiveType"
					PrimitiveType.DOUBLE -> "Double::class.javaPrimitiveType"
					PrimitiveType.OBJECT -> "Any::class.java"
					PrimitiveType.ARRAY -> "Array::class.java"
					PrimitiveType.VOID -> "Void::class.javaPrimitiveType"
					else -> throw JadxRuntimeException("Unknown or null primitive type: $type")
				}
			}
			else -> "\"$type\""
		}
	}

	private fun generateClassSnippet(jClass: JClass): String {
		val javaClass = jClass.cls
		val rawClassName = javaClass.rawName
		val className = javaClass.name

		val template = when (language) {
			XposedCodegenLanguage.JAVA -> "Class<?> %sClass = classLoader.loadClass(\"%s\");"
			XposedCodegenLanguage.KOTLIN -> "val %sClass = classLoader.loadClass(\"%s\")"
		}

		return String.format(template, className, rawClassName)
	}

	private fun generateFieldSnippet(jField: JField): String {
		val javaField = jField.javaField
		val static = if (javaField.accessFlags.isStatic) "Static" else ""
		val type = PRIMITIVE_TYPE_MAPPING.getOrDefault(javaField.fieldNode.type.toString(), "Object")
		val xposedMethod = "XposedHelpers.get${static}${type}Field"

		val template = when (language) {
			XposedCodegenLanguage.JAVA -> "%s(/*runtimeObject*/, \"%s\");"
			XposedCodegenLanguage.KOTLIN -> "%s(/*runtimeObject*/, \"%s\")"
		}

		return String.format(template, xposedMethod, javaField.fieldNode.fieldInfo.name)
	}

	private val language: XposedCodegenLanguage
		get() = getCodeArea().mainWindow.settings.xposedCodegenLanguage

	companion object {
		private val LOG: Logger = LoggerFactory.getLogger(XposedAction::class.java)
		private const val serialVersionUID = 2641585141624592578L

		private val PRIMITIVE_TYPE_MAPPING = mapOf(
			"int" to "Int",
			"byte" to "Byte",
			"short" to "Short",
			"long" to "Long",
			"float" to "Float",
			"double" to "Double",
			"char" to "Char",
			"boolean" to "Boolean",
		)
	}
}
