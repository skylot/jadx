package jadx.plugins.script.runtime.data

import jadx.core.dex.nodes.BlockNode
import jadx.core.dex.nodes.InsnNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.regions.Region
import jadx.plugins.script.runtime.JadxScriptInstance

class Stages(private val jadx: JadxScriptInstance) {

	fun prepare(block: (RootNode) -> Unit) {
		jadx.addPass(object : ScriptPreparePass(jadx, "StagePrepare") {
			override fun init(root: RootNode) {
				jadx.debug.catchExceptions("Prepare init block") {
					block.invoke(root)
				}
			}
		})
	}

	fun rawInsns(block: (MethodNode, Array<InsnNode?>) -> Unit) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageRawInsns",
			runAfter = listOf("start"),
		) {
			override fun visit(mth: MethodNode) {
				mth.instructions?.let {
					jadx.debug.catchExceptions("Method instructions visit") {
						block.invoke(mth, it)
					}
				}
			}
		})
	}

	fun mthEarlyBlocks(block: (MethodNode, List<BlockNode>) -> Unit) {
		mthBlocks(beforePass = "SSATransform", block)
	}

	fun mthBlocks(
		beforePass: String = "RegionMakerVisitor",
		block: (MethodNode, List<BlockNode>) -> Unit,
	) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageMthBlocks",
			runBefore = listOf(beforePass),
		) {
			override fun visit(mth: MethodNode) {
				mth.basicBlocks?.let {
					jadx.debug.catchExceptions("Method blocks visit") {
						block.invoke(mth, it)
					}
				}
			}
		})
	}

	fun mthRegions(block: (MethodNode, Region) -> Unit) {
		jadx.addPass(object : ScriptOrderedDecompilePass(
			jadx,
			"StageMthRegions",
			runBefore = listOf("PrepareForCodeGen"),
		) {
			override fun visit(mth: MethodNode) {
				mth.region?.let {
					jadx.debug.catchExceptions("Method region visit") {
						block.invoke(mth, it)
					}
				}
			}
		})
	}
}
