package jadx.dex.visitors.regions;

import jadx.dex.instructions.InsnType;
import jadx.dex.instructions.args.ArgType;
import jadx.dex.nodes.BlockNode;
import jadx.dex.nodes.IBlock;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.regions.LoopRegion;

import java.util.Iterator;
import java.util.List;

public class FinishRegions extends TracedRegionVisitor {
	@Override
	public void processBlockTraced(MethodNode mth, IBlock container, IRegion currentRegion) {
		if (container.getClass() != BlockNode.class)
			return;

		BlockNode block = (BlockNode) container;

		// remove last return in void functions
		if (block.getCleanSuccessors().isEmpty()
				&& mth.getReturnType().equals(ArgType.VOID)) {
			List<InsnNode> insns = block.getInstructions();
			int lastIndex = insns.size() - 1;
			if (lastIndex != -1) {
				InsnNode last = insns.get(lastIndex);
				if (last.getType() == InsnType.RETURN
						&& blockNotInLoop(mth, block)) {
					insns.remove(lastIndex);
				}
			}
		}
	}

	private boolean blockNotInLoop(MethodNode mth, BlockNode block) {
		if (mth.getLoopForBlock(block) != null)
			return false;

		for (Iterator<IRegion> it = regionStack.descendingIterator(); it.hasNext(); ) {
			IRegion region = it.next();
			if (region.getClass() == LoopRegion.class)
				return false;
		}
		return true;
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			LoopRegion loop = (LoopRegion) region;
			loop.mergePreCondition();
		}
		super.leaveRegion(mth, region);
	}
}
