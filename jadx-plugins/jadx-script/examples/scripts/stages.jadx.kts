// insert processing passes for different decompilation stages

import jadx.core.dex.instructions.InsnType
import jadx.core.dex.nodes.IRegion
import java.lang.Integer.max

val jadx = getJadxInstance()

// print raw instructions
jadx.stages.rawInsns { mth, insns ->
	log.info { "Instructions for method: $mth" }
	for ((offset, insn) in insns.withIndex()) {
		insn?.let {
			log.info { " 0x${offset.hex()}: $insn" }
		}
	}
}

// access method basic blocks
jadx.stages.mthBlocks { mth, blocks ->
	// count invoke instructions
	var invCount = 0
	for (block in blocks) {
		for (insn in block.instructions) {
			if (insn.type == InsnType.INVOKE) {
				invCount++
			}
		}
	}
	log.info { "Invokes count in method $mth = $invCount" }
}

// access method regions
jadx.stages.mthRegions { mth, region ->
	// recursively count max depth of nested regions
	fun countRegionsDepth(region: IRegion): Int {
		val subBlocks = region.subBlocks
		if (subBlocks.isEmpty()) {
			return 0
		}
		var depth = 1
		for (block in subBlocks) {
			if (block is IRegion) {
				depth = max(depth, 1 + countRegionsDepth(block))
			}
		}
		return depth
	}

	val depth = countRegionsDepth(region)
	log.info { "Max region depth in method $mth = $depth" }
	if (depth > 5) {
		jadx.debug.printMethodRegions(mth, printInsns = true)
	}
}

jadx.afterLoad {
	/*
		Start full decompilation (optional):
		1. jadx-cli start decompilation automatically
		2. jadx-gui start decompilation only on class open or search, so you might need to force it
	*/
	// jadx.decompile.all()
}
