package jadx.core.dex.visitors.regions;

import java.util.List;
import java.util.ListIterator;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IBranchRegion;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.dex.regions.loops.LoopRegion;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.exceptions.JadxException;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Remove unnecessary return instructions for void methods
 */
public class ReturnVisitor extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) throws JadxException {
		if (mth.isVoidReturn()) {
			DepthRegionTraversal.traverse(mth, new ReturnRemoverVisitor());
		}
	}

	private static final class ReturnRemoverVisitor extends TracedRegionVisitor {

		@Override
		public boolean enterRegion(MethodNode mth, IRegion region) {
			super.enterRegion(mth, region);
			return !(region instanceof SwitchRegion);
		}

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
					insns.remove(0);
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
				if (region instanceof IBranchRegion) {
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
						} else if (!isEmpty(subBlock)) {
							return false;
						}
					}
				}
				curContainer = region;
			}
			return true;
		}

		/**
		 * Check if container not contains instructions,
		 * don't count one 'return' instruction (it will be removed later).
		 */
		private static boolean isEmpty(IContainer container) {
			if (container instanceof IBlock) {
				IBlock block = (IBlock) container;
				return block.getInstructions().isEmpty() || block.contains(AFlag.RETURN);
			} else if (container instanceof IRegion) {
				IRegion region = (IRegion) container;
				for (IContainer block : region.getSubBlocks()) {
					if (!isEmpty(block)) {
						return false;
					}
				}
				return true;
			} else {
				throw new JadxRuntimeException("Unknown container type: " + container.getClass());
			}
		}
	}
}
