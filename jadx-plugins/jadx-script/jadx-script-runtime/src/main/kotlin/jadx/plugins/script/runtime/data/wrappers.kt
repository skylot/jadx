package jadx.plugins.script.runtime.data

import jadx.api.core.nodes.IClassNode
import jadx.api.core.nodes.IMethodNode
import jadx.api.core.nodes.IRootNode
import jadx.api.plugins.pass.JadxPass
import jadx.api.plugins.pass.impl.OrderedJadxPassInfo
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo
import jadx.api.plugins.pass.types.JadxDecompilePass
import jadx.api.plugins.pass.types.JadxPreparePass
import jadx.plugins.script.runtime.JadxScriptInstance

private fun buildScriptName(jadx: JadxScriptInstance, name: String) = "JadxScript${name}(${jadx.scriptName})"

private fun buildSimplePassInfo(jadx: JadxScriptInstance, name: String) =
	SimpleJadxPassInfo(buildScriptName(jadx, name))

abstract class ScriptPreparePass(
	private val jadx: JadxScriptInstance, private val name: String
) : JadxPreparePass {
	override fun getInfo() = buildSimplePassInfo(jadx, name)
}

abstract class ScriptDecompilePass(
	private val jadx: JadxScriptInstance, private val name: String
) : JadxDecompilePass {
	override fun getInfo() = buildSimplePassInfo(jadx, name)

	override fun init(root: IRootNode) {
	}

	override fun visit(cls: IClassNode): Boolean {
		return true
	}

	override fun visit(mth: IMethodNode) {
	}
}

abstract class ScriptOrderedPass(
	private val jadx: JadxScriptInstance,
	private val name: String,
	private val runAfter: List<String> = listOf(),
	private val runBefore: List<String> = listOf()
) : JadxPass {
	override fun getInfo(): OrderedJadxPassInfo {
		val scriptName = buildScriptName(jadx, name)
		return OrderedJadxPassInfo(scriptName, scriptName, runAfter, runBefore)
	}
}

abstract class ScriptOrderedPreparePass(
	jadx: JadxScriptInstance, name: String, runAfter: List<String> = listOf(), runBefore: List<String> = listOf()
) : ScriptOrderedPass(jadx, name, runAfter, runBefore), JadxPreparePass {}

abstract class ScriptOrderedDecompilePass(
	jadx: JadxScriptInstance, name: String, runAfter: List<String> = listOf(), runBefore: List<String> = listOf()
) : ScriptOrderedPass(jadx, name, runAfter, runBefore), JadxDecompilePass {

	override fun init(root: IRootNode) {
	}

	override fun visit(cls: IClassNode): Boolean {
		return true
	}

	override fun visit(mth: IMethodNode) {
	}
}
