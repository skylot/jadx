package jadx.plugins.kotlin.metadata.utils

import jadx.core.Consts
import jadx.core.dex.info.FieldInfo
import jadx.core.dex.instructions.IndexInsnNode
import jadx.core.dex.instructions.InsnType
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.instructions.args.PrimitiveType
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.plugins.kotlin.metadata.model.MethodRename
import jadx.plugins.kotlin.metadata.model.ToStringRename
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import java.util.Locale

object KotlinUtils {

	fun isDataClass(kmCls: KmClass): Boolean {
		return Flag.Class.IS_DATA(kmCls.flags)
	}

	fun parseToString(cls: ClassNode): ToStringRename? {
		val mthToString = cls.searchMethodByShortId(Consts.MTH_TOSTRING_SIGNATURE)
			?: return null

		return ToStringParser.parse(mthToString)
	}

	fun findGetters(cls: ClassNode): List<MethodRename> {
		return cls.fields.filter(FieldNode::isInstance).mapNotNull { field ->
			val mth = getFieldGetterMethod(cls, field.fieldInfo)
				?: return@mapNotNull null
			MethodRename(
				mth = mth,
				alias = getGetterAlias(field.alias),
			)
		}
	}

	private fun getFieldGetterMethod(cls: ClassNode, field: FieldInfo): MethodNode? {
		return cls.methods.firstOrNull {
			it.returnType == field.type &&
				it.argTypes.isEmpty() &&
				it.insnsCount == 3 &&
				it.sVars.size == 2 &&
				(it.sVars[1].assignInsn as? IndexInsnNode)?.index == field
		}
	}

	private fun getGetterAlias(fieldAlias: String): String {
		val capitalized = fieldAlias.replaceFirstChar {
			if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
		}
		return "get$capitalized"
	}

	// untested & overly complicated
	fun parseDefaultMethods(cls: ClassNode): List<MethodRename> {
		val possibleMthList = cls.methods.filter {
			it.accessFlags.isStatic && it.accessFlags.isSynthetic &&
				it.argTypes.run {
					size > 3 &&
						first().isObject && first().`object` == cls.fullName &&
						get(size - 2).isPrimitive && get(size - 2).primitiveType == PrimitiveType.INT &&
						last().isObject && last().`object` == Consts.CLASS_OBJECT
				}
		}
		val insnList = possibleMthList.filter {
			it.exitBlock.run {
				iDom != null && iDom.instructions.firstOrNull()?.type == InsnType.RETURN
				iDom.iDom != null
			} &&
				it.exitBlock.iDom.iDom.run {
					instructions.firstOrNull() is InvokeNode
				}
		}

		val remapped = insnList.mapNotNull {
			val insn = it.exitBlock.iDom.iDom.instructions.first() as InvokeNode
			cls.searchMethodByShortId(insn.callMth.shortId)?.run { it to this }
		}

		return remapped.map { (defaultMethod, originalMethod) ->
			MethodRename(
				mth = defaultMethod,
				alias = getDefaultMethodAlias(originalMethod.alias),
			)
		}
	}

	private fun getDefaultMethodAlias(alias: String): String {
		return "$alias\$default"
	}
}
