package jadx.core.dex.instructions;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;

public abstract class TargetInsnNode extends InsnNode {

	public TargetInsnNode(InsnType type, int argsCount) {
		super(type, argsCount);
	}

	public void initBlocks(BlockNode curBlock) {
	}

	public boolean replaceTargetBlock(BlockNode origin, BlockNode replace) {
		return false;
	}
}
