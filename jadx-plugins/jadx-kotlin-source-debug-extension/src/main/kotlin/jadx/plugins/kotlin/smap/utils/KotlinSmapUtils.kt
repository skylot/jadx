package jadx.plugins.kotlin.smap.utils

import jadx.core.deobf.NameMapper
import jadx.core.dex.attributes.nodes.RenameReasonAttr
import jadx.core.dex.nodes.ClassNode
import jadx.core.utils.Utils
import jadx.plugins.kotlin.smap.model.ClassAliasRename
import jadx.plugins.kotlin.smap.model.SMAP
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.java

object KotlinSmapUtils {

	val LOG: Logger = LoggerFactory.getLogger(KotlinSmapUtils::class.java)

	@JvmStatic
	fun getClassAlias(cls: ClassNode): ClassAliasRename? {
		val annotation = cls.getSourceDebugExtension() ?: return null
		return getClassAlias(cls, annotation)
	}

	private fun getClassAlias(cls: ClassNode, annotation: SMAP): ClassAliasRename? {
		val firstValue = annotation.fileMappings[0].path.replace("/", ".")
		try {
			val clsName = firstValue.trim()
				.takeUnless(String::isEmpty)
				?.let(Utils::cleanObjectName)
				?: return null

			val alias = splitAndCheckClsName(cls, clsName)
			if (alias != null) {
				RenameReasonAttr.forNode(cls).append("from SourceDebugExtension")
				return alias
			}
		} catch (e: Exception) {
			LOG.error("Failed to parse SourceDebugExtension", e)
		}
		return null
	}

	// Don't use ClassInfo facility to not pollute class into cache
	private fun splitAndCheckClsName(originCls: ClassNode, fullClsName: String): ClassAliasRename? {
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
		if (originName == name || name.contains("$") ||
			!NameMapper.isValidIdentifier(name) || pkg.startsWith("java.")
		) {
			return null
		}
		val newClsNode = originCls.root().resolveClass(fullClsName)
		return if (newClsNode != null) {
			// class with alias name already exist
			null
		} else {
			ClassAliasRename(pkg, name)
		}
	}
}
