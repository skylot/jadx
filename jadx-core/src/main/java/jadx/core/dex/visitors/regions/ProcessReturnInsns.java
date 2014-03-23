package jadx.core.dex.visitors.regions;

import jadx.core.dex.attributes.AttributeFlag;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.IBlock;
import jadx.core.dex.nodes.IContainer;
import jadx.core.dex.nodes.IRegion;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.regions.IfRegion;
import jadx.core.dex.regions.LoopRegion;
import jadx.core.dex.regions.SwitchRegion;
import jadx.core.utils.RegionUtils;

import java.util.List;
import java.util.ListIterator;

/**
 * Remove unnecessary return instructions for void methods
 */
public class ProcessReturnInsns extends TracedRegionVisitor {

	@Override
	public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
		if (container.getClass() != BlockNode.class) {
			return;
		}
		BlockNode block = (BlockNode) container;
		if (block.getAttributes().contains(AttributeFlag.RETURN)) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.size() == 1
					&& blockNotInLoop(mth, block)
					&& noTrailInstructions(block)) {
				insns.remove(insns.size() - 1);
				block.getAttributes().remove(AttributeFlag.RETURN);
			}
		}
	}

	private boolean blockNotInLoop(MethodNode mth, BlockNode block) {
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
	 * Check that there no code after this block in regions structure
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
					} else if (!subBlock.getAttributes().contains(AttributeFlag.RETURN)
							&& RegionUtils.notEmpty(subBlock)) {
						return false;
					}
				}
			}
			curContainer = region;
		}
		return true;
	}
}
