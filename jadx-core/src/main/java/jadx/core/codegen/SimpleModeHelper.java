package jadx.core.codegen;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.IfNode;
import jadx.core.dex.instructions.TargetInsnNode;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.visitors.blocks.BlockProcessor;
import jadx.core.dex.visitors.blocks.BlockSplitter;
import jadx.core.utils.BlockUtils;

public class SimpleModeHelper {

	private final MethodNode mth;

	private final BitSet startLabel;
	private final BitSet endGoto;

	public SimpleModeHelper(MethodNode mth) {
		this.mth = mth;
		this.startLabel = BlockUtils.newBlocksBitSet(mth);
		this.endGoto = BlockUtils.newBlocksBitSet(mth);
	}

	public List<BlockNode> prepareBlocks() {
		removeEmptyBlocks();
		List<BlockNode> blocksList = getSortedBlocks();
		blocksList.removeIf(b -> b.equals(mth.getEnterBlock()) || b.equals(mth.getExitBlock()));
		unbindExceptionHandlers();
		if (blocksList.isEmpty()) {
			return Collections.emptyList();
		}
		@Nullable
		BlockNode prev = null;
		int blocksCount = blocksList.size();
		for (int i = 0; i < blocksCount; i++) {
			BlockNode block = blocksList.get(i);
			BlockNode nextBlock = i + 1 == blocksCount ? null : blocksList.get(i + 1);
			List<BlockNode> preds = block.getPredecessors();
			int predsCount = preds.size();
			if (predsCount > 1) {
				startLabel.set(block.getId());
			} else if (predsCount == 1 && prev != null) {
				if (!prev.equals(preds.get(0))) {
					if (!block.contains(AFlag.EXC_BOTTOM_SPLITTER)) {
						startLabel.set(block.getId());
					}
					if (prev.getSuccessors().size() == 1 && !mth.isPreExitBlocks(prev)) {
						endGoto.set(prev.getId());
					}
				}
			}
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn instanceof TargetInsnNode) {
				processTargetInsn(block, lastInsn, nextBlock);
			}
			if (block.contains(AType.EXC_HANDLER)) {
				startLabel.set(block.getId());
			}
			if (nextBlock == null && !mth.isPreExitBlocks(block)) {
				endGoto.set(block.getId());
			}
			prev = block;
		}
		if (mth.isVoidReturn()) {
			int last = blocksList.size() - 1;
			if (blocksList.get(last).contains(AFlag.RETURN)) {
				// remove trailing return
				blocksList.remove(last);
			}
		}
		return blocksList;
	}

	private void removeEmptyBlocks() {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getInstructions().isEmpty()
					&& block.getPredecessors().size() > 0
					&& block.getSuccessors().size() == 1) {
				BlockNode successor = block.getSuccessors().get(0);
				List<BlockNode> predecessors = block.getPredecessors();
				BlockSplitter.removeConnection(block, successor);
				if (predecessors.size() == 1) {
					BlockSplitter.replaceConnection(predecessors.get(0), block, successor);
				} else {
					for (BlockNode pred : new ArrayList<>(predecessors)) {
						BlockSplitter.replaceConnection(pred, block, successor);
					}
				}
				block.add(AFlag.REMOVE);
			}
		}
		BlockProcessor.removeMarkedBlocks(mth);
	}

	private void unbindExceptionHandlers() {
		if (mth.isNoExceptionHandlers()) {
			return;
		}
		for (ExceptionHandler handler : mth.getExceptionHandlers()) {
			BlockNode handlerBlock = handler.getHandlerBlock();
			if (handlerBlock != null) {
				BlockSplitter.removePredecessors(handlerBlock);
			}
		}
	}

	private void processTargetInsn(BlockNode block, InsnNode lastInsn, @Nullable BlockNode next) {
		if (lastInsn instanceof IfNode) {
			IfNode ifInsn = (IfNode) lastInsn;
			BlockNode thenBlock = ifInsn.getThenBlock();
			if (Objects.equals(next, thenBlock)) {
				ifInsn.invertCondition();
				startLabel.set(ifInsn.getThenBlock().getId());
			} else {
				startLabel.set(thenBlock.getId());
			}
			ifInsn.normalize();
		} else {
			for (BlockNode successor : block.getSuccessors()) {
				startLabel.set(successor.getId());
			}
		}
	}

	public boolean isNeedStartLabel(BlockNode block) {
		return startLabel.get(block.getId());
	}

	public boolean isNeedEndGoto(BlockNode block) {
		return endGoto.get(block.getId());
	}

	// DFS sort blocks to reduce goto count
	private List<BlockNode> getSortedBlocks() {
		List<BlockNode> list = new ArrayList<>(mth.getBasicBlocks().size());
		BlockUtils.dfsVisit(mth, list::add);
		return list;
	}
}
