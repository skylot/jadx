package jadx.plugins.script.runtime.data

import jadx.core.dex.instructions.args.InsnArg
import jadx.core.dex.instructions.args.InsnWrapArg
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.utils.InsnRemover
import jadx.plugins.script.runtime.JadxScriptInstance

class Replace(private val jadx: JadxScriptInstance) {

	fun insns(replace: (MethodNode, InsnNode) -> InsnNode?) {
		jadx.stages.mthBlocks { mth, blocks ->
			for (block in blocks) {
				val insns = block.instructions
				for ((i, insn) in insns.withIndex()) {
					replaceSubInsns(mth, insn, replace)
					replace.invoke(mth, insn)?.let {
						insns[i] = it
					}
				}
			}
		}
	}

	private fun replaceSubInsns(mth: MethodNode, insn: InsnNode, replace: (MethodNode, InsnNode) -> InsnNode?) {
		val argsCount = insn.argsCount
		if (argsCount == 0) {
			return
		}
		for (i in 0 until argsCount) {
			val arg = insn.getArg(i)
			if (arg is InsnWrapArg) {
				val wrapInsn = arg.wrapInsn
				replaceSubInsns(mth, wrapInsn, replace)
				replace.invoke(mth, wrapInsn)?.let {
					InsnRemover.unbindArgUsage(mth, arg)
					insn.setArg(i, InsnArg.wrapInsnIntoArg(it))
				}
			}
		}
	}
}
