package jadx.core.utils.kotlin

import jadx.core.deobf.NameMapper
import jadx.core.dex.attributes.nodes.RenameReasonAttr
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.utils.Utils
import jadx.core.utils.kmCls
import jadx.core.utils.log.LOG
import jadx.core.utils.shortId
import kotlinx.metadata.KmClass

object KotlinMetadataUtils {

	@JvmStatic
	fun getAlias(cls: ClassNode): ClsAliasPair? {
		val annotation = cls.getMetadata() ?: return null
		return getClassAlias(cls, annotation)
	}

	@JvmStatic
	fun getMetadataResult(cls: ClassNode): ClsMetadataResult? {
		val kmCls = cls.kmCls ?: return null
		var methodArgs: Map<MethodNode, List<MethodArgRename>> = emptyMap()
		var fields: Map<FieldNode, String> = emptyMap()

		if (kmCls.companionObject != null) {
			LOG.info("${cls.fullName}: Companion -> ${kmCls.companionObject}")
		}

		try {
			methodArgs = mapMethodArgs(cls, kmCls)
		} catch (t: Throwable) {
			LOG.error("Failed to parse kotlin metadata method args", t)
		}
		try {
			fields = mapFields(cls, kmCls)
		} catch (t: Throwable) {
			LOG.error("Failed to parse kotlin metadata fields", t)
		}

		return ClsMetadataResult(
			methodArgs = methodArgs,
			fields = fields
		)
	}

	/**
	 * Try to get class info from Kotlin Metadata annotation
	 */
	private fun getClassAlias(cls: ClassNode, annotation: Metadata): ClsAliasPair? {
		val firstValue = annotation.data2.getOrNull(0) ?: return null

		try {
			val clsName = firstValue.trim()
				.takeUnless(String::isEmpty)
				?.let(Utils::cleanObjectName)
				?: return null

			val alias = splitAndCheckClsName(cls, clsName)
			if (alias != null) {
				RenameReasonAttr.forNode(cls).append("from Kotlin metadata")
				return alias
			}
		} catch (e: Exception) {
			LOG.error("Failed to parse kotlin metadata", e)
		}
		return null
	}

	// Don't use ClassInfo facility to not pollute class into cache
	private fun splitAndCheckClsName(originCls: ClassNode, fullClsName: String): ClsAliasPair? {
		if (!NameMapper.isValidFullIdentifier(fullClsName)) {
			return null
		}
		val pkg: String
		val name: String
		val dot = fullClsName.lastIndexOf('.')
		if (dot == -1) {
			pkg = ""
			name = fullClsName
		} else {
			pkg = fullClsName.substring(0, dot)
			name = fullClsName.substring(dot + 1)
		}
		val originClsInfo = originCls.classInfo
		val originName = originClsInfo.shortName
		if (originName == name || name.contains("$")
			|| !NameMapper.isValidIdentifier(name)
			|| countPkgParts(originClsInfo.getPackage()) != countPkgParts(pkg) || pkg.startsWith("java.")
		) {
			return null
		}
		val newClsNode = originCls.root().resolveClass(fullClsName)
		return if (newClsNode != null) {
			// class with alias name already exist
			null
		} else ClsAliasPair(pkg, name)
	}

	private fun countPkgParts(pkg: String): Int {
		if (pkg.isEmpty()) {
			return 0
		}
		var count = 1
		var pos = 0
		while (true) {
			pos = pkg.indexOf('.', pos)
			if (pos == -1) {
				return count
			}
			pos++
			count++
		}
	}

	private fun mapMethodArgs(cls: ClassNode, kmCls: KmClass): Map<MethodNode, List<MethodArgRename>> {
		return buildMap {
			kmCls.functions.forEach { kmFunction ->
				val node: MethodNode? = cls.searchMethodByShortId(kmFunction.shortId)
				if (node == null || node.isNoCode) return@forEach

				val argCount = node.argTypes.size
				val paramCount = kmFunction.valueParameters.size
				if (argCount == paramCount) {
					// requires arg registers to be loaded, is this necessary ?
					val aliasList = node.argRegs.zip(kmFunction.valueParameters).map { (rArg, kmValueParameter) ->
						MethodArgRename(rArg = rArg, alias = kmValueParameter.name)
					}
					put(node, aliasList)
				}
			}
		}
	}

	private fun mapFields(cls: ClassNode, kmCls: KmClass): Map<FieldNode, String> {
		return buildMap {
			kmCls.properties.forEach { kmProperty ->
				val node = cls.searchFieldByShortId(kmProperty.shortId) ?: return@forEach
				put(node, kmProperty.name)
			}
		}
	}
}

