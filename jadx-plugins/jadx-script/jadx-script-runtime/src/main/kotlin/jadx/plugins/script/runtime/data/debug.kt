package jadx.plugins.script.runtime.data

import jadx.api.core.nodes.IMethodNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.visitors.DotGraphVisitor
import jadx.core.utils.DebugUtils
import jadx.plugins.script.runtime.JadxScriptInstance
import java.io.File

class Debug(private val jadx: JadxScriptInstance) {

	fun printMethodRegions(mth: IMethodNode, printInsns: Boolean = false) {
		DebugUtils.printRegions(mth as MethodNode, printInsns)
	}

	fun saveCFG(mth: IMethodNode, file: File = File("dump-mth-raw")) {
		DotGraphVisitor.dumpRaw().save(file, mth as MethodNode)
	}
}
