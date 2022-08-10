// instructions modification example

import jadx.core.dex.instructions.ConstStringNode
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.instructions.args.InsnArg

val jadx = getJadxInstance()

jadx.replace.insns { mth, insn ->
	if (insn is InvokeNode) {
		if (insn.callMth.shortId == "println(Ljava/lang/String;)V") {
			val arg = insn.getArg(1)
			val newArg = InsnArg.wrapInsnIntoArg(ConstStringNode("Jadx!"))
			insn.setArg(1, newArg)
			log.info { "Replace '$arg' with '$newArg' in $mth" }
		}
	}
	null
}
