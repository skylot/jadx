package jadx.dex.visitors.regions;

import jadx.dex.instructions.InsnType;
import jadx.dex.nodes.IBlock;
import jadx.dex.nodes.IRegion;
import jadx.dex.nodes.InsnNode;
import jadx.dex.nodes.MethodNode;
import jadx.dex.regions.LoopRegion;

import java.util.List;

public class FinishRegions implements IRegionVisitor {

	@Override
	public void processBlock(MethodNode mth, IBlock block) {

		// remove return from class init method
		if (mth.getMethodInfo().isClassInit()) {
			List<InsnNode> insns = block.getInstructions();
			if (insns.size() != 0) {
				InsnNode last = insns.get(insns.size() - 1);
				if (last.getType() == InsnType.RETURN) {
					insns.remove(insns.size() - 1);
				}
			}
		}
	}

	@Override
	public void enterRegion(MethodNode mth, IRegion region) {
	}

	@Override
	public void leaveRegion(MethodNode mth, IRegion region) {
		if (region instanceof LoopRegion) {
			LoopRegion loop = (LoopRegion) region;
			loop.mergePreCondition();
		}
	}

}
