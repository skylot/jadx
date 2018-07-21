package jadx.core.dex.instructions;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.utils.InsnUtils;

public class GotoNode extends TargetInsnNode {

	protected final int target;

	public GotoNode(int target) {
		this(InsnType.GOTO, target, 0);
	}

	protected GotoNode(InsnType type, int target, int argsCount) {
		super(type, argsCount);
		this.target = target;
	}

	public int getTarget() {
		return target;
	}

	@Override
	public boolean replaceTargetBlock(BlockNode origin, BlockNode replace) {
		return false;
	}

	@Override
	public void initBlocks(BlockNode curBlock) {
	}

	@Override
	public String toString() {
		return super.toString() + "-> " + InsnUtils.formatOffset(target);
	}
}
