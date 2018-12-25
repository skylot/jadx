package jadx.core.dex.visitors.blocksmaker;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;

public class BlockFinish extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(BlockFinish.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode()) {
			return;
		}

		for (BlockNode block : mth.getBasicBlocks()) {
			block.updateCleanSuccessors();
			fixSplitterBlock(mth, block);
		}

		mth.finishBasicBlocks();
	}

	/**
	 * For evey exception handler must be only one splitter block,
	 * select correct one and remove others if necessary.
	 */
	private static void fixSplitterBlock(MethodNode mth, BlockNode block) {
		ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
		if (excHandlerAttr == null) {
			return;
		}
		BlockNode handlerBlock = excHandlerAttr.getHandler().getHandlerBlock();
		if (handlerBlock.getPredecessors().size() < 2) {
			return;
		}
		Map<BlockNode, SplitterBlockAttr> splitters = new HashMap<>();
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
			mth.addWarn("Unknown top exception splitter block from list: " + splitters);
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
