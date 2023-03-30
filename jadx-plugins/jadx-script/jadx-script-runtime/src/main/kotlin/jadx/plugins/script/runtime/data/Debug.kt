package jadx.plugins.script.runtime.data

import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.visitors.DotGraphVisitor
import jadx.core.utils.DebugUtils
import jadx.plugins.script.runtime.JadxScriptInstance
import java.io.File

class Debug(private val jadx: JadxScriptInstance) {

	fun printMethodRegions(mth: MethodNode, printInsns: Boolean = false) {
		DebugUtils.printRegions(mth, printInsns)
	}

	fun saveCFG(mth: MethodNode, file: File = File("dump-mth-raw")) {
		DotGraphVisitor.dumpRaw().save(file, mth)
	}

	fun printPreparePasses() {
		jadx.internalDecompiler.root.preDecompilePasses.forEach { jadx.log.info { it.name } }
	}

	fun printPasses() {
		jadx.internalDecompiler.root.passes.forEach { jadx.log.info { it.name } }
	}
}
