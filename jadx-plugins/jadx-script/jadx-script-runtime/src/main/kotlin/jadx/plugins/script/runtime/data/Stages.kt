package jadx.plugins.script.runtime.data

import jadx.core.dex.nodes.BlockNode
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.regions.Region
import jadx.plugins.script.runtime.JadxScriptInstance

class Stages(private val jadx: JadxScriptInstance) {

	fun rawInsns(block: (MethodNode, Array<InsnNode?>) -> Unit) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageRawInsns",
			runAfter = listOf("start")
		) {
				override fun visit(mth: MethodNode) {
					mth.instructions?.let {
						block.invoke(mth, it)
					}
				}
			})
	}

	fun mthEarlyBlocks(block: (MethodNode, List<BlockNode>) -> Unit) {
		mthBlocks(beforePass = "SSATransform", block)
	}

	fun mthBlocks(
		beforePass: String = "RegionMakerVisitor",
		block: (MethodNode, List<BlockNode>) -> Unit
	) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageMthBlocks",
			runBefore = listOf(beforePass)
		) {
				override fun visit(mth: MethodNode) {
					mth.basicBlocks?.let {
						block.invoke(mth, it)
					}
				}
			})
	}

	fun mthRegions(block: (MethodNode, Region) -> Unit) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageMthRegions",
			runBefore = listOf("PrepareForCodeGen")
		) {
				override fun visit(mth: MethodNode) {
					mth.region?.let {
						block.invoke(mth, it)
					}
				}
			})
	}
}
