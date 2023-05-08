package jadx.core.utils.kotlin

import jadx.api.plugins.input.data.AccessFlags
import jadx.core.Consts
import jadx.core.dex.attributes.AFlag
import jadx.core.dex.instructions.IndexInsnNode
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.MethodNode
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClass
import java.util.Locale

object KotlinDataClassUtils {

	fun fixDataClass(cls: ClassNode, kmCls: KmClass) {
		val isData = Flag.Class.IS_DATA(kmCls.flags)
		var changed = false

		if (isData != cls.accessFlags.isData) {
			cls.accessFlags = cls.accessFlags.run {
				if (isData) add(AccessFlags.DATA) else remove(AccessFlags.DATA)
			}
			changed = true
		}

		val mthToString: MethodNode? = cls.searchMethodByShortId(Consts.MTH_TOSTRING_SIGNATURE)
		if (mthToString != null) {
			val trans = DataClassTransform(mthToString)

			trans.clsAlias?.let { alias ->
				if (alias != cls.alias) {
					cls.rename(alias)
					changed = true
				}
			}

			trans.list.forEach { (alias, fieldInfo) ->
				// rename inner field
				val field = cls.searchFieldByShortId(fieldInfo.shortId) ?: return@forEach
				if (alias != field.alias) {
					field.rename(alias)
					changed = true
				}

				// find getter method
				cls.methods.firstOrNull {
					it.returnType == field.type &&
							it.argTypes.isEmpty() &&
							it.insnsCount == 3 &&
							it.sVars.size == 2 &&
							(it.sVars[1].assignInsn as? IndexInsnNode)?.index == fieldInfo
				}?.let { getter ->
					getter.rename(getGetterAlias(alias))
					changed = true
				}

			}

		}

		if (!changed) {
			cls.add(AFlag.DONT_GENERATE)
		}
	}

	private fun getGetterAlias(fieldAlias: String): String {
		val capitalized = fieldAlias.replaceFirstChar {
			if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
		}
		return "get$capitalized"
	}
}
