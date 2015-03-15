package jadx.core.dex.visitors.blocksmaker;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlockFinish extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(BlockFinish.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		for (BlockNode block : mth.getBasicBlocks()) {
			block.updateCleanSuccessors();
			initBlocksInIfNodes(block);
			fixSplitterBlock(block);
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

	/**
	 * For evey exception handler must be only one splitter block,
	 * select correct one and remove others if necessary.
	 */
	private static void fixSplitterBlock(BlockNode block) {
		ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			return;
		}
		BlockNode handlerBlock = excHandlerAttr.getHandler().getHandlerBlock();
		if (handlerBlock.getPredecessors().size() < 2) {
			return;
		}
		Map<BlockNode, SplitterBlockAttr> splitters = new HashMap<BlockNode, SplitterBlockAttr>();
		for (BlockNode pred : handlerBlock.getPredecessors()) {
			pred = BlockUtils.skipSyntheticPredecessor(pred);
			SplitterBlockAttr splitterAttr = pred.get(AType.SPLITTER_BLOCK);
			if (splitterAttr != null && pred == splitterAttr.getBlock()) {
				splitters.put(pred, splitterAttr);
			}
		}
		if (splitters.size() < 2) {
			return;
		}
		BlockNode topSplitter = BlockUtils.getTopBlock(splitters.keySet());
		if (topSplitter == null) {
			LOG.warn("Unknown top splitter block from list: {}", splitters);
			return;
		}
		for (Map.Entry<BlockNode, SplitterBlockAttr> entry : splitters.entrySet()) {
			BlockNode pred = entry.getKey();
			SplitterBlockAttr splitterAttr = entry.getValue();
			if (pred == topSplitter) {
				block.addAttr(splitterAttr);
			} else {
				pred.remove(AType.SPLITTER_BLOCK);
				for (BlockNode s : pred.getCleanSuccessors()) {
					s.remove(AType.SPLITTER_BLOCK);
				}
			}
		}
	}
}
