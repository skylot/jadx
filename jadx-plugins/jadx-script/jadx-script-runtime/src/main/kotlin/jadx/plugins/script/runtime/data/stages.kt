package jadx.plugins.script.runtime.data

import jadx.api.core.nodes.IMethodNode
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
			override fun visit(mth: IMethodNode) {
				val mthNode = mth as MethodNode
				mthNode.instructions?.let {
					block.invoke(mthNode, it)
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
			override fun visit(mth: IMethodNode) {
				val mthNode = mth as MethodNode
				mthNode.basicBlocks?.let {
					block.invoke(mthNode, it)
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
			override fun visit(mth: IMethodNode) {
				val mthNode = mth as MethodNode
				mthNode.region?.let {
					block.invoke(mthNode, it)
				}
			}
		})
	}
}
