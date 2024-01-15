package jadx.core.dex.visitors.blocks;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Duplicate code to resolve java jsr/ret.
 * JSR (jump subroutine) allows executing the same code from different places.
 * Used mostly for 'finally' blocks, deprecated in Java 7.
 */
public class ResolveJavaJSR {

	public static void process(MethodNode mth) {
		int blocksCount = mth.getBasicBlocks().size();
		int k = 0;
		while (true) {
			boolean changed = resolve(mth);
			if (!changed) {
				break;
			}
			if (k++ > blocksCount) {
				throw new JadxRuntimeException("Fail to resolve jsr instructions");
			}
		}
	}

	private static boolean resolve(MethodNode mth) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		int blocksCount = blocks.size();
		for (BlockNode block : blocks) {
			if (BlockUtils.checkLastInsnType(block, InsnType.JAVA_RET)) {
				resolveForRetBlock(mth, block);
				if (blocksCount != mth.getBasicBlocks().size()) {
					return true;
				}
			}
		}
		return false;
	}

	private static void resolveForRetBlock(MethodNode mth, BlockNode retBlock) {
		BlockUtils.visitPredecessorsUntil(mth, retBlock, startBlock -> {
			List<BlockNode> preds = startBlock.getPredecessors();
			if (preds.size() > 1
					&& preds.stream().allMatch(p -> BlockUtils.checkLastInsnType(p, InsnType.JAVA_JSR))) {
				List<BlockNode> jsrBlocks = new ArrayList<>(preds);
				List<BlockNode> dupBlocks = BlockUtils.collectAllSuccessors(mth, startBlock, false);
				removeInsns(retBlock, startBlock, jsrBlocks);
				processBlocks(mth, retBlock, startBlock, jsrBlocks, dupBlocks);
				return true;
			}
			return false;
		});
	}

	private static void removeInsns(BlockNode retBlock, BlockNode startBlock, List<BlockNode> jsrBlocks) {
		InsnNode retInsn = ListUtils.removeLast(retBlock.getInstructions());
		if (retInsn != null && retInsn.getType() == InsnType.JAVA_RET) {
			InsnArg retArg = retInsn.getArg(0);
			if (retArg.isRegister()) {
				int regNum = ((RegisterArg) retArg).getRegNum();
				InsnNode startInsn = BlockUtils.getFirstInsn(startBlock);
				if (startInsn != null
						&& startInsn.getType() == InsnType.MOVE
						&& startInsn.getResult().getRegNum() == regNum) {
					startBlock.getInstructions().remove(0);
				}
			}
		}
		jsrBlocks.forEach(p -> ListUtils.removeLast(p.getInstructions()));
	}

	private static void processBlocks(MethodNode mth, BlockNode retBlock, BlockNode startBlock,
			List<BlockNode> jsrBlocks, List<BlockNode> dupBlocks) {
		BlockNode first = null;
		for (BlockNode jsrBlock : jsrBlocks) {
			if (first == null) {
				first = jsrBlock;
			} else {
				BlockNode pathBlock = BlockUtils.selectOther(startBlock, jsrBlock.getSuccessors());
				BlockSplitter.removeConnection(jsrBlock, startBlock);
				BlockSplitter.removeConnection(jsrBlock, pathBlock);
				List<BlockNode> newBlocks = BlockSplitter.copyBlocksTree(mth, dupBlocks);
				BlockNode newStart = newBlocks.get(dupBlocks.indexOf(startBlock));
				BlockNode newRetBlock = newBlocks.get(dupBlocks.indexOf(retBlock));
				BlockSplitter.connect(jsrBlock, newStart);
				BlockSplitter.connect(newRetBlock, pathBlock);
			}
		}
		if (first != null) {
			BlockNode pathBlock = BlockUtils.selectOther(startBlock, first.getSuccessors());
			BlockSplitter.removeConnection(first, pathBlock);
			BlockSplitter.connect(retBlock, pathBlock);
		}
	}
}
