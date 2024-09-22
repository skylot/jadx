package jadx.core.dex.visitors.regions.maker;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.Region;
import jadx.core.dex.regions.SynchronizedRegion;
import jadx.core.dex.visitors.regions.CleanRegions;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.Utils;

import static jadx.core.utils.BlockUtils.getNextBlock;
import static jadx.core.utils.BlockUtils.isPathExists;

public class SynchronizedRegionMaker {
	private static final Logger LOG = LoggerFactory.getLogger(SynchronizedRegionMaker.class);
	private final MethodNode mth;
	private final RegionMaker regionMaker;

	SynchronizedRegionMaker(MethodNode mth, RegionMaker regionMaker) {
		this.mth = mth;
		this.regionMaker = regionMaker;
	}

	BlockNode process(IRegion curRegion, BlockNode block, InsnNode insn, RegionStack stack) {
		SynchronizedRegion synchRegion = new SynchronizedRegion(curRegion, insn);
		synchRegion.getSubBlocks().add(block);
		curRegion.getSubBlocks().add(synchRegion);

		Set<BlockNode> exits = new LinkedHashSet<>();
		Set<BlockNode> cacheSet = new HashSet<>();
		traverseMonitorExits(synchRegion, insn.getArg(0), block, exits, cacheSet);

		for (InsnNode exitInsn : synchRegion.getExitInsns()) {
			BlockNode insnBlock = BlockUtils.getBlockByInsn(mth, exitInsn);
			if (insnBlock != null) {
				insnBlock.add(AFlag.DONT_GENERATE);
			}
			// remove arg from MONITOR_EXIT to allow inline in MONITOR_ENTER
			exitInsn.removeArg(0);
			exitInsn.add(AFlag.DONT_GENERATE);
		}

		BlockNode body = getNextBlock(block);
		if (body == null) {
			mth.addWarn("Unexpected end of synchronized block");
			return null;
		}
		BlockNode exit = null;
		if (exits.size() == 1) {
			exit = getNextBlock(exits.iterator().next());
		} else if (exits.size() > 1) {
			cacheSet.clear();
			exit = traverseMonitorExitsCross(body, exits, cacheSet);
		}

		stack.push(synchRegion);
		if (exit != null) {
			stack.addExit(exit);
		} else {
			for (BlockNode exitBlock : exits) {
				// don't add exit blocks which leads to method end blocks ('return', 'throw', etc)
				List<BlockNode> list = BlockUtils.buildSimplePath(exitBlock);
				if (list.isEmpty() || !BlockUtils.isExitBlock(mth, Utils.last(list))) {
					stack.addExit(exitBlock);
					// we can still try using this as an exit block to make sure it's visited.
					exit = exitBlock;
				}
			}
		}
		synchRegion.getSubBlocks().add(regionMaker.makeRegion(body));
		stack.pop();
		return exit;
	}

	/**
	 * Traverse from monitor-enter thru successors and collect blocks contains monitor-exit
	 */
	private static void traverseMonitorExits(SynchronizedRegion region, InsnArg arg, BlockNode block, Set<BlockNode> exits,
			Set<BlockNode> visited) {
		visited.add(block);
		for (InsnNode insn : block.getInstructions()) {
			if (insn.getType() == InsnType.MONITOR_EXIT
					&& insn.getArgsCount() > 0
					&& insn.getArg(0).equals(arg)) {
				exits.add(block);
				region.getExitInsns().add(insn);
				return;
			}
		}
		for (BlockNode node : block.getSuccessors()) {
			if (!visited.contains(node)) {
				traverseMonitorExits(region, arg, node, exits, visited);
			}
		}
	}

	/**
	 * Traverse from monitor-enter thru successors and search for exit paths cross
	 */
	private static BlockNode traverseMonitorExitsCross(BlockNode block, Set<BlockNode> exits, Set<BlockNode> visited) {
		visited.add(block);
		for (BlockNode node : block.getCleanSuccessors()) {
			boolean cross = true;
			for (BlockNode exitBlock : exits) {
				boolean p = isPathExists(exitBlock, node);
				if (!p) {
					cross = false;
					break;
				}
			}
			if (cross) {
				return node;
			}
			if (!visited.contains(node)) {
				BlockNode res = traverseMonitorExitsCross(node, exits, visited);
				if (res != null) {
					return res;
				}
			}
		}
		return null;
	}

	public static void removeSynchronized(MethodNode mth) {
		Region startRegion = mth.getRegion();
		List<IContainer> subBlocks = startRegion.getSubBlocks();
		if (!subBlocks.isEmpty() && subBlocks.get(0) instanceof SynchronizedRegion) {
			SynchronizedRegion synchRegion = (SynchronizedRegion) subBlocks.get(0);
			InsnNode synchInsn = synchRegion.getEnterInsn();
			if (!synchInsn.getArg(0).isThis()) {
				LOG.warn("In synchronized method {}, top region not synchronized by 'this' {}", mth, synchInsn);
				return;
			}
			// replace synchronized block with inner region
			startRegion.getSubBlocks().set(0, synchRegion.getRegion());
			// remove 'monitor-enter' instruction
			InsnRemover.remove(mth, synchInsn);
			// remove 'monitor-exit' instruction
			for (InsnNode exit : synchRegion.getExitInsns()) {
				InsnRemover.remove(mth, exit);
			}
			// run region cleaner again
			CleanRegions.process(mth);
			// assume that CodeShrinker will be run after this
		}
	}
}
