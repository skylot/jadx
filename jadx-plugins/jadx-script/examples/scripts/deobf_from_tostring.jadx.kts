/*
 Rename class and fields using strings from toString() method
*/

import jadx.core.deobf.NameMapper
import jadx.core.dex.attributes.AFlag
import jadx.core.dex.attributes.nodes.RenameReasonAttr
import jadx.core.dex.info.FieldInfo
import jadx.core.dex.instructions.ConstStringNode
import jadx.core.dex.instructions.IndexInsnNode
import jadx.core.dex.instructions.InsnType
import jadx.core.dex.instructions.args.InsnWrapArg
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.MethodNode
import jadx.plugins.script.runtime.data.ScriptOrderedDecompilePass

val jadx = getJadxInstance()

// StringBuilder chain replaced by STR_CONCAT instruction in SimplifyVisitor
// Search for return with STR_CONCAT and process args
jadx.addPass(object : ScriptOrderedDecompilePass(
	jadx,
	"DeobfFromToString",
	runAfter = listOf("SimplifyVisitor"),
) {
	override fun visit(mth: MethodNode) {
		if (mth.methodInfo.shortId == "toString()Ljava/lang/String;") {
			val returnBlock = mth.exitBlock.predecessors.firstOrNull { it.contains(AFlag.RETURN) }
			val lastInsn = returnBlock?.instructions?.lastOrNull()
			if (lastInsn != null && lastInsn.type == InsnType.RETURN) {
				val arg = lastInsn.getArg(0)
				if (arg.isInsnWrap) {
					val wrapInsn = (arg as InsnWrapArg).wrapInsn
					if (wrapInsn.type == InsnType.STR_CONCAT) {
						log.info { "Renaming using 'toString' in class: ${mth.parentClass}" }
						processArgs(mth, wrapInsn)
					}
				}
			}
		}
	}

	val clsSepRgx = Regex("[ ({:]")

	private fun processArgs(mth: MethodNode, wrapInsn: InsnNode): Boolean {
		try {
			var fldName: String? = null
			for ((i, arg) in wrapInsn.arguments.withIndex()) {
				val insn = arg.unwrap() ?: return false
				if (i % 2 == 0) {
					if (insn !is ConstStringNode) {
						return false
					}
					var str = insn.string
					if (i == 0) {
						// class and first field name
						val parts = str.split(clsSepRgx)
						val clsName = parts[0]
						if (NameMapper.isValidIdentifier(clsName)) {
							mth.parentClass.run {
								log.info { "rename class '$name' to '$clsName'" }
								rename(clsName)
								RenameReasonAttr.forNode(this).append("from toString()")
							}
						}
						str = parts[1]
					}
					fldName = str.trim('\'', '=', ',', ' ', ':')
				} else {
					if (insn.type != InsnType.IGET) {
						return false
					}
					val iget = insn as IndexInsnNode
					val fldInfo = iget.index as FieldInfo
					val fld = mth.parentClass.searchField(fldInfo)
					if (fld != null && NameMapper.isValidIdentifier(fldName)) {
						log.info { "rename field '${fld.name}' to '$fldName'" }
						fld.rename(fldName)
						RenameReasonAttr.forNode(fld).append("from toString()")
					}
				}
			}
			return true
		} catch (e: Exception) {
			log.error(e) { "Args process failed" }
			return false
		}
	}
})
