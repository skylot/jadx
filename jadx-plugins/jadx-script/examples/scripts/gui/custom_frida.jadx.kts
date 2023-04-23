@file:DependsOn("org.apache.commons:commons-text:1.10.0")

import jadx.api.metadata.ICodeNodeRef
import jadx.core.codegen.TypeGen
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.utils.exceptions.JadxRuntimeException
import org.apache.commons.text.StringEscapeUtils

val jadx = getJadxInstance()

jadx.gui.ifAvailable {
	addPopupMenuAction(
		"Custom Frida snippet (g)",
		enabled = ::isActionEnabled,
		keyBinding = "G",
		action = ::runAction,
	)
}

fun isActionEnabled(node: ICodeNodeRef): Boolean {
	return node is MethodNode || node is ClassNode || node is FieldNode
}

fun runAction(node: ICodeNodeRef) {
	try {
		val fridaSnippet = generateFridaSnippet(node)
		log.info { "Custom frida snippet:\n$fridaSnippet" }
		jadx.gui.copyToClipboard(fridaSnippet)
	} catch (e: Exception) {
		log.error(e) { "Failed to generate Frida code snippet" }
	}
}

fun generateFridaSnippet(node: ICodeNodeRef): String {
	return when (node) {
		is MethodNode -> generateMethodSnippet(node)
		is ClassNode -> generateClassSnippet(node)
		is FieldNode -> generateFieldSnippet(node)
		else -> throw JadxRuntimeException("Unsupported node type: " + node.javaClass)
	}
}

fun generateClassSnippet(cls: ClassNode): String {
	return """let ${cls.name} = Java.use("${StringEscapeUtils.escapeEcmaScript(cls.rawName)}");"""
}

fun generateMethodSnippet(mthNode: MethodNode): String {
	val methodInfo = mthNode.methodInfo
	val methodName = if (methodInfo.isConstructor) {
		"\$init"
	} else {
		StringEscapeUtils.escapeEcmaScript(methodInfo.name)
	}
	val overload = if (isOverloaded(mthNode)) {
		".overload(${methodInfo.argumentsTypes.joinToString(transform = this::parseArgType)})"
	} else {
		""
	}
	val shortClassName = mthNode.parentClass.name
	val argNames = mthNode.collectArgsWithoutLoading().map { a -> a.name }
	val args = argNames.joinToString(separator = ", ")
	val logArgs = if (argNames.isNotEmpty()) {
		argNames.joinToString(separator = " + ', ' + ", prefix = " + ', ' + ") { p -> "'$p: ' + $p" }
	} else {
		""
	}
	val clsSnippet = generateClassSnippet(mthNode.parentClass)
	return if (methodInfo.isConstructor || methodInfo.returnType == ArgType.VOID) {
		// no return value
		"""
		$clsSnippet
		$shortClassName["$methodName"]$overload.implementation = function ($args) {
			console.log('$shortClassName.$methodName is called'$logArgs);
			this["$methodName"]($args);
		};
		""".trimIndent()
	} else {
		"""
		$clsSnippet
		$shortClassName["$methodName"]$overload.implementation = function ($args) {
			console.log('$shortClassName.$methodName is called'$logArgs);
			let ret = this["$methodName"]($args);
			console.log('$shortClassName.$methodName return: ' + ret);
			return ret;
		};
		""".trimIndent()
	}
}

fun generateFieldSnippet(fld: FieldNode): String {
	var rawFieldName = StringEscapeUtils.escapeEcmaScript(fld.name)
	for (methodNode in fld.parentClass.methods) {
		if (methodNode.name == rawFieldName) {
			rawFieldName = "_$rawFieldName"
			break
		}
	}
	return """
		${generateClassSnippet(fld.parentClass)}
		${fld.name} = ${fld.parentClass.name}.$rawFieldName.value;
	""".trimIndent()
}

fun isOverloaded(methodNode: MethodNode): Boolean {
	return methodNode.parentClass.methods.stream().anyMatch { m: MethodNode ->
		m.name == methodNode.name && methodNode.methodInfo.shortId != m.methodInfo.shortId
	}
}

fun parseArgType(x: ArgType): String {
	val typeStr = if (x.isArray) {
		TypeGen.signature(x).replace("/", ".")
	} else {
		x.toString()
	}
	return "'$typeStr'"
}
