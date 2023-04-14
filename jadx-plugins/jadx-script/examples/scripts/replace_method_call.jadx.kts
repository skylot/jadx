/**
 * Replace method call with calculated result.
 * Useful for custom string deobfuscation.
 *
 * Example for sample from issue https://github.com/skylot/jadx/issues/1251
 */

import jadx.core.dex.instructions.ConstStringNode
import jadx.core.dex.instructions.InvokeNode
import jadx.core.dex.instructions.args.InsnArg
import jadx.core.dex.instructions.args.InsnWrapArg
import jadx.core.dex.instructions.args.RegisterArg


val jadx = getJadxInstance()

val mthSignature = "com.xshield.aa.iIiIiiiiII(Ljava/lang/String;)Ljava/lang/String;"

jadx.replace.insns { mth, insn ->
	if (insn is InvokeNode && insn.callMth.rawFullId == mthSignature) {
		val str = getConstStr(insn.getArg(0))
		if (str != null) {
			val resultStr = decode(str)
			log.info { "Decode '$str' to '$resultStr' in $mth" }
			return@insns ConstStringNode(resultStr)
		}
	}
	null
}

fun getConstStr(arg: InsnArg): String? {
	val insn = when (arg) {
		is InsnWrapArg -> arg.wrapInsn
		is RegisterArg -> arg.assignInsn
		else -> null
	}
	if (insn is ConstStringNode) {
		return insn.string
	}
	return null
}

/**
 * Decompiled method, automatically converted to Kotlin by IntelliJ Idea
 */
fun decode(str: String): String {
	val length = str.length
	val cArr = CharArray(length)
	var i = length - 1
	while (i >= 0) {
		val i2 = i - 1
		cArr[i] = (str[i].code xor 'z'.code).toChar()
		if (i2 < 0) {
			break
		}
		i = i2 - 1
		cArr[i2] = (str[i2].code xor '\u000c'.code).toChar()
	}
	return String(cArr)
}
