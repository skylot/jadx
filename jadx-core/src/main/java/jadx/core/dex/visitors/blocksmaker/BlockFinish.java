package jadx.core.dex.visitors.blocksmaker;

import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.AbstractVisitor;

import java.util.List;

public class BlockFinish extends AbstractVisitor {

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		for (BlockNode block : mth.getBasicBlocks()) {
			block.updateCleanSuccessors();
			initBlocksInIfNodes(block);
		}

		mth.finishBasicBlocks();
	}

	/**
	 * Init 'then' and 'else' blocks for 'if' instruction.
	 */
	private static void initBlocksInIfNodes(BlockNode block) {
		List<InsnNode> instructions = block.getInstructions();
		if (instructions.size() == 1) {
			InsnNode insn = instructions.get(0);
			if (insn.getType() == InsnType.IF) {
				((IfNode) insn).initBlocks(block);
			}
		}
	}
}
