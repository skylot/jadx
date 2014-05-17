package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.RegionUtils;
import jadx.core.utils.exceptions.JadxException;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Remove unnecessary return instructions for void methods
 */
public class ReturnVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		// remove useless returns in void methods
		if (mth.getReturnType().equals(ArgType.VOID)) {
			DepthRegionTraversal.traverseAll(mth, new Process());
		}
	}

	private static final class Process extends TracedRegionVisitor {
		@Override
		public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
			if (container.getClass() != BlockNode.class) {
				return;
			}
			BlockNode block = (BlockNode) container;
			if (block.contains(AFlag.RETURN)) {
				List<InsnNode> insns = block.getInstructions();
				if (insns.size() == 1
						&& blockNotInLoop(mth, block)
						&& noTrailInstructions(block)) {
					insns.remove(insns.size() - 1);
					block.remove(AFlag.RETURN);
				}
			}
		}

		private boolean blockNotInLoop(MethodNode mth, BlockNode block) {
			if (mth.getLoopsCount() == 0) {
				return true;
			}
			if (mth.getLoopForBlock(block) != null) {
				return false;
			}
			for (IRegion region : regionStack) {
				if (region.getClass() == LoopRegion.class) {
					return false;
				}
			}
			return true;
		}

		/**
		 * Check that there are no code after this block in regions structure
		 */
		private boolean noTrailInstructions(BlockNode block) {
			IContainer curContainer = block;
			for (IRegion region : regionStack) {
				// ignore paths on other branches
				if (region instanceof IfRegion
						|| region instanceof SwitchRegion) {
					curContainer = region;
					continue;
				}
				List<IContainer> subBlocks = region.getSubBlocks();
				if (!subBlocks.isEmpty()) {
					ListIterator<IContainer> itSubBlock = subBlocks.listIterator(subBlocks.size());
					while (itSubBlock.hasPrevious()) {
						IContainer subBlock = itSubBlock.previous();
						if (subBlock == curContainer) {
							break;
						} else if (notEmpty(subBlock)) {
							return false;
						}
					}
				}
				curContainer = region;
			}
			return true;
		}

		private static boolean notEmpty(IContainer subBlock) {
			if (subBlock.contains(AFlag.RETURN)) {
				return false;
			}
			int insnCount = RegionUtils.insnsCount(subBlock);
			if (insnCount > 1) {
				return true;
			}
			if (insnCount == 1) {
				// don't count one 'return' instruction (it will be removed later)
				Set<BlockNode> blocks = new HashSet<BlockNode>();
				RegionUtils.getAllRegionBlocks(subBlock, blocks);
				for (BlockNode node : blocks) {
					if (!node.contains(AFlag.RETURN) && !node.getInstructions().isEmpty()) {
						return true;
					}
				}
			}
			return false;
		}
	}
}
