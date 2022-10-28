/*
 Rename method if specific string is found
*/

import jadx.api.plugins.input.insns.Opcode
import jadx.core.dex.nodes.MethodNode

val renamesMap  = mapOf(
	"specificString" to "newMethodName",
	"AA6" to "aa6Method"
)

val jadx = getJadxInstance()

var n = 0
jadx.rename.all { _, node ->
	var newName : String? = null
	if (node is MethodNode) {
		// use quick instructions scanner
		node.codeReader?.visitInstructions { insn ->
			if (insn.opcode == Opcode.CONST_STRING) {
				insn.decode()
				val constStr = insn.indexAsString
				val renameStr = renamesMap[constStr]
				if (renameStr != null) {
					log.info { "Found '$constStr' in method $node, renaming to '$renameStr'" }
					newName = renameStr
					n++
				}
			}
		}
	}
	newName
}

jadx.afterLoad {
	log.info { "Script '$scriptName' renamed $n methods" }
}
