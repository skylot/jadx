package jadx.core.dex.visitors.blocksmaker;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.IgnoreEdgeAttr;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.blocksmaker.helpers.BlocksPair;
import jadx.core.dex.visitors.blocksmaker.helpers.BlocksRemoveInfo;
import jadx.core.dex.visitors.ssa.LiveVarAnalysis;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InstructionRemover;
import jadx.core.utils.exceptions.JadxRuntimeException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.connect;
import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.insertBlockBetween;
import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.removeConnection;

public class BlockFinallyExtract extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(BlockFinallyExtract.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.isNoExceptionHandlers()) {
			return;
		}

		boolean reloadBlocks = false;
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		for (int i = 0; i < basicBlocks.size(); i++) {
			BlockNode block = basicBlocks.get(i);
			if (processExceptionHandler(mth, block)) {
				reloadBlocks = true;
			}
		}
		if (reloadBlocks) {
			mergeReturnBlocks(mth);
			BlockProcessor.rerun(mth);
		}
	}

	private static boolean processExceptionHandler(MethodNode mth, BlockNode block) {
		ExcHandlerAttr handlerAttr = block.get(AType.EXC_HANDLER);
		if (handlerAttr == null) {
			return false;
		}
		ExceptionHandler excHandler = handlerAttr.getHandler();

		// check if handler has exit edge to block not from this handler
		boolean noExitNode = true;
		boolean reThrowRemoved = false;

		for (BlockNode excBlock : excHandler.getBlocks()) {
			if (noExitNode) {
				noExitNode = excHandler.getBlocks().containsAll(excBlock.getCleanSuccessors());
			}
			List<InsnNode> insns = excBlock.getInstructions();
			int size = insns.size();
			if (excHandler.isCatchAll()
					&& size != 0
					&& insns.get(size - 1).getType() == InsnType.THROW) {
				reThrowRemoved = true;
				InstructionRemover.remove(mth, excBlock, size - 1);
			}
		}
		if (reThrowRemoved && noExitNode
				&& extractFinally(mth, block, excHandler)) {
			return true;
		}
		int totalSize = countInstructions(excHandler);
		if (totalSize == 0 && reThrowRemoved && noExitNode) {
			handlerAttr.getTryBlock().removeHandler(mth, excHandler);
		}
		return false;
	}

	/**
	 * Search and remove common code from 'catch' and 'handlers'.
	 */
	private static boolean extractFinally(MethodNode mth, BlockNode handlerBlock, ExceptionHandler handler) {
		int count = handler.getBlocks().size();
		BitSet bs = new BitSet(count);
		List<BlockNode> blocks = new ArrayList<BlockNode>(count);
		for (BlockNode block : handler.getBlocks()) {
			List<InsnNode> insns = block.getInstructions();
			if (!insns.isEmpty()) {
				if (insns.get(0).getType() != InsnType.MOVE_EXCEPTION) {
					blocks.add(block);
				}
				bs.set(block.getId());
			}
		}
		if (blocks.isEmpty()) {
			// nothing to do
			return false;
		}

		List<BlocksRemoveInfo> removes = new LinkedList<BlocksRemoveInfo>();
		Set<BlockNode> splitters = new HashSet<BlockNode>();

		// remove 'finally' from handlers
		TryCatchBlock tryBlock = handler.getTryBlock();
		if (tryBlock.getHandlersCount() > 1) {
			for (ExceptionHandler otherHandler : tryBlock.getHandlers()) {
				if (otherHandler == handler) {
					continue;
				}
				for (BlockNode hb : otherHandler.getBlocks()) {
					BlocksRemoveInfo removeInfo = removeInsns(mth, hb, blocks, bs);
					if (removeInfo != null) {
						removes.add(removeInfo);
						break;
					}
				}
			}
			if (removes.size() != tryBlock.getHandlersCount() - 1) {
				return false;
			}
		}

		for (ExceptionHandler otherHandler : tryBlock.getHandlers()) {
			SplitterBlockAttr splitterAttr = otherHandler.getHandlerBlock().get(AType.SPLITTER_BLOCK);
			if (splitterAttr != null) {
				BlockNode splBlock = splitterAttr.getBlock();
				if (!splBlock.getCleanSuccessors().isEmpty()) {
					splitters.add(splBlock);
				}
			}
		}

		// remove 'finally' from 'try' blocks (dominated by splitter block)
		boolean removed = false;
		for (BlockNode splitter : splitters) {
			BlockNode start = splitter.getCleanSuccessors().get(0);
			List<BlockNode> list = BlockUtils.collectBlocksDominatedBy(splitter, start);
			for (BlockNode block : list) {
				if (bs.get(block.getId())) {
					continue;
				}
				BlocksRemoveInfo removeInfo = removeInsns(mth, block, blocks, bs);
				if (removeInfo != null) {
					removes.add(removeInfo);
					removed = true;
					break;
				}
			}
		}
		if (!removed) {
			return false;
		}

		// 'finally' extract confirmed
		for (BlocksRemoveInfo removeInfo : removes) {
			if (!applyRemove(mth, removeInfo)) {
				return false;
			}
		}
		handler.setFinally(true);
		// remove 'move-exception' instruction
		if (BlockUtils.checkLastInsnType(handlerBlock, InsnType.MOVE_EXCEPTION)) {
			InstructionRemover.remove(mth, handlerBlock, handlerBlock.getInstructions().size() - 1);
			handlerBlock.add(AFlag.SKIP);
		}
		return true;
	}

	private static BlocksRemoveInfo removeInsns(MethodNode mth, BlockNode remBlock, List<BlockNode> blocks, BitSet bs) {
		if (blocks.isEmpty()) {
			return null;
		}
		BlockNode startBlock = blocks.get(0);
		BlocksRemoveInfo removeInfo = checkFromFirstBlock(remBlock, startBlock, bs);
		if (removeInfo == null) {
			return null;
		}
		if (removeInfo.getOuts().size() != 1) {
			LOG.debug("Unexpected finally block outs count: {}", removeInfo.getOuts());
			return null;
		}
		return removeInfo;
	}

	private static BlocksRemoveInfo checkFromFirstBlock(BlockNode remBlock, BlockNode startBlock, BitSet bs) {
		BlocksRemoveInfo removeInfo = isStartBlock(remBlock, startBlock);
		if (removeInfo == null) {
			return null;
		}
		if (!checkBlocksTree(remBlock, startBlock, removeInfo, bs)) {
			return null;
		}
		return removeInfo;
	}

	/**
	 * 'Finally' instructions can start in the middle of the first block.
	 */
	private static BlocksRemoveInfo isStartBlock(BlockNode remBlock, BlockNode startBlock) {
		List<InsnNode> remInsns = remBlock.getInstructions();
		List<InsnNode> startInsns = startBlock.getInstructions();
		if (remInsns.size() < startInsns.size()) {
			return null;
		}
		// first - fast check
		int delta = remInsns.size() - startInsns.size();
		if (!checkInsns(remInsns, startInsns, delta, null)) {
			return null;
		}
		BlocksRemoveInfo removeInfo = new BlocksRemoveInfo(new BlocksPair(remBlock, startBlock));
		removeInfo.setStartSplitIndex(delta);
		// second - run checks again for collect registers mapping
		if (!checkInsns(remInsns, startInsns, delta, removeInfo)) {
			return null;
		}
		return removeInfo;
	}

	private static boolean checkInsns(List<InsnNode> remInsns, List<InsnNode> startInsns, int delta,
			@Nullable BlocksRemoveInfo removeInfo) {
		for (int i = startInsns.size() - 1; i >= 0; i--) {
			InsnNode startInsn = startInsns.get(i);
			InsnNode remInsn = remInsns.get(delta + i);
			if (!sameInsns(remInsn, startInsn, removeInfo)) {
				return false;
			}
		}
		return true;
	}

	private static boolean checkBlocksTree(BlockNode remBlock, BlockNode startBlock, BlocksRemoveInfo removeInfo,
			BitSet bs) {
		// skip check on start block
		if (!removeInfo.getProcessed().isEmpty()
				&& !sameBlocks(remBlock, startBlock, removeInfo)) {
			return false;
		}
		removeInfo.getProcessed().add(new BlocksPair(remBlock, startBlock));

		List<BlockNode> baseCS = startBlock.getCleanSuccessors();
		List<BlockNode> remCS = remBlock.getCleanSuccessors();
		if (baseCS.size() != remCS.size()) {
			removeInfo.getOuts().add(new BlocksPair(remBlock, startBlock));
			return true;
		}
		for (int i = 0; i < baseCS.size(); i++) {
			BlockNode sBlock = baseCS.get(i);
			BlockNode rBlock = remCS.get(i);
			if (bs.get(sBlock.getId())) {
				if (!checkBlocksTree(rBlock, sBlock, removeInfo, bs)) {
					return false;
				}
			} else {
				removeInfo.getOuts().add(new BlocksPair(rBlock, sBlock));
			}
		}
		return true;
	}

	private static boolean sameBlocks(BlockNode remBlock, BlockNode startBlock, BlocksRemoveInfo removeInfo) {
		List<InsnNode> first = remBlock.getInstructions();
		List<InsnNode> second = startBlock.getInstructions();
		if (first.size() != second.size()) {
			return false;
		}
		int size = first.size();
		for (int i = 0; i < size; i++) {
			if (!sameInsns(first.get(i), second.get(i), removeInfo)) {
				return false;
			}
		}
		return true;
	}

	private static boolean sameInsns(InsnNode remInsn, InsnNode fInsn, BlocksRemoveInfo removeInfo) {
		if (remInsn.getType() != fInsn.getType()
				|| remInsn.getArgsCount() != fInsn.getArgsCount()) {
			return false;
		}
		for (int i = 0; i < remInsn.getArgsCount(); i++) {
			InsnArg remArg = remInsn.getArg(i);
			InsnArg fArg = fInsn.getArg(i);
			if (remArg.isRegister() != fArg.isRegister()) {
				return false;
			}
			if (removeInfo != null && fArg.isRegister()) {
				RegisterArg remReg = (RegisterArg) remArg;
				RegisterArg fReg = (RegisterArg) fArg;
				if (remReg.getRegNum() != fReg.getRegNum()) {
					RegisterArg mapReg = removeInfo.getRegMap().get(remArg);
					if (mapReg == null) {
						removeInfo.getRegMap().put(remReg, fReg);
					} else if (!mapReg.equalRegisterAndType(fReg)) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private static boolean applyRemove(MethodNode mth, BlocksRemoveInfo removeInfo) {
		BlockNode remBlock = removeInfo.getStart().getFirst();
		BlockNode startBlock = removeInfo.getStart().getSecond();

		if (remBlock.contains(AFlag.REMOVE)) {
			// already processed
			return true;
		}
		if (remBlock.getPredecessors().size() != 1) {
			LOG.warn("Finally extract failed: remBlock pred: {}, {}", remBlock, remBlock.getPredecessors());
			return false;
		}
		BlockNode remBlockPred = remBlock.getPredecessors().get(0);
		int splitIndex = removeInfo.getStartSplitIndex();
		if (splitIndex > 0) {
			// split start block (remBlock)
			BlockNode newBlock = insertBlockBetween(mth, remBlockPred, remBlock);
			for (int i = 0; i < splitIndex; i++) {
				InsnNode insnNode = remBlock.getInstructions().get(i);
				insnNode.add(AFlag.SKIP);
				newBlock.getInstructions().add(insnNode);
			}
			Iterator<InsnNode> it = remBlock.getInstructions().iterator();
			while (it.hasNext()) {
				InsnNode insnNode = it.next();
				if (insnNode.contains(AFlag.SKIP)) {
					it.remove();
				}
			}
			for (InsnNode insnNode : newBlock.getInstructions()) {
				insnNode.remove(AFlag.SKIP);
			}
			remBlockPred = newBlock;
		}

		BlocksPair out = removeInfo.getOuts().iterator().next();
		BlockNode rOut = out.getFirst();
		BlockNode sOut = out.getSecond();

		// redirect out edges
		List<BlockNode> filtPreds = BlockUtils.filterPredecessors(sOut);
		if (filtPreds.size() > 1) {
			BlockNode pred = sOut.getPredecessors().get(0);
			BlockNode newPred = BlockSplitter.insertBlockBetween(mth, pred, sOut);
			for (BlockNode predBlock : new ArrayList<BlockNode>(sOut.getPredecessors())) {
				if (predBlock != newPred) {
					removeConnection(predBlock, sOut);
					connect(predBlock, newPred);
				}
			}
			rOut.getPredecessors().clear();
			addIgnoredEdge(newPred, rOut);
			connect(newPred, rOut);
		} else if (filtPreds.size() == 1) {
			BlockNode pred = filtPreds.get(0);
			BlockNode repl = removeInfo.getBySecond(pred);
			if (repl == null) {
				throw new JadxRuntimeException("Block not found by " + pred
						+ ", in " + removeInfo + ", method: " + mth);
			}
			removeConnection(pred, rOut);
			addIgnoredEdge(repl, rOut);
			connect(repl, rOut);
		} else {
			throw new JadxRuntimeException("Finally extract failed, unexpected preds: " + filtPreds
					+ " for " + sOut + ", method: " + mth);
		}

		// redirect input edges
		for (BlockNode pred : new ArrayList<BlockNode>(remBlock.getPredecessors())) {
			removeConnection(pred, remBlock);
			connect(pred, startBlock);
			addIgnoredEdge(pred, startBlock);
			connect(pred, rOut);
		}

		// generate 'move' instruction for mapped register pairs
		if (!removeInfo.getRegMap().isEmpty()) {
			// TODO: very expensive operation
			LiveVarAnalysis la = new LiveVarAnalysis(mth);
			la.runAnalysis();
			for (Map.Entry<RegisterArg, RegisterArg> entry : removeInfo.getRegMap().entrySet()) {
				RegisterArg from = entry.getKey();
				if (la.isLive(remBlockPred.getId(), from.getRegNum())) {
					RegisterArg to = entry.getValue();
					InsnNode move = new InsnNode(InsnType.MOVE, 1);
					move.setResult(to);
					move.addArg(from);
					remBlockPred.getInstructions().add(move);
				}
			}
		}

		// mark blocks for remove
		markForRemove(remBlock);
		for (BlocksPair pair : removeInfo.getProcessed()) {
			markForRemove(pair.getFirst());
			BlockNode second = pair.getSecond();
			second.updateCleanSuccessors();
		}
		return true;
	}

	/**
	 * Unbind block for removing.
	 */
	private static void markForRemove(BlockNode block) {
		for (BlockNode p : block.getPredecessors()) {
			p.getSuccessors().remove(block);
		}
		for (BlockNode s : block.getSuccessors()) {
			s.getPredecessors().remove(block);
		}
		block.getPredecessors().clear();
		block.getSuccessors().clear();
		block.add(AFlag.REMOVE);
	}

	private static void addIgnoredEdge(BlockNode from, BlockNode toBlock) {
		IgnoreEdgeAttr edgeAttr = from.get(AType.IGNORE_EDGE);
		if (edgeAttr == null) {
			edgeAttr = new IgnoreEdgeAttr();
			from.addAttr(edgeAttr);
		}
		edgeAttr.getBlocks().add(toBlock);
	}

	private static int countInstructions(ExceptionHandler excHandler) {
		int totalSize = 0;
		for (BlockNode excBlock : excHandler.getBlocks()) {
			List<InsnNode> list = excBlock.getInstructions();
			if (!list.isEmpty() && list.get(0).getType() == InsnType.MOVE_EXCEPTION) {
				// don't count 'move-exception' it will be removed later
				totalSize--;
			}
			totalSize += list.size();
		}
		return totalSize;
	}

	/**
	 * Merge return block with same predecessor.
	 */
	private static void mergeReturnBlocks(MethodNode mth) {
		List<BlockNode> exitBlocks = mth.getExitBlocks();
		BlockNode pred = getFinallyOutBlock(exitBlocks);
		if (pred == null) {
			return;
		}
		IgnoreEdgeAttr edgeAttr = pred.get(AType.IGNORE_EDGE);
		if (edgeAttr == null) {
			return;
		}
		List<BlockNode> merge = new LinkedList<BlockNode>();
		for (BlockNode blockNode : pred.getSuccessors()) {
			if (blockNode.contains(AFlag.RETURN)) {
				merge.add(blockNode);
			}
		}
		if (merge.size() < 2) {
			return;
		}
		// select 'original' return block
		BlockNode origReturnBlock = null;
		for (BlockNode ret : merge) {
			if (ret.contains(AFlag.ORIG_RETURN)) {
				origReturnBlock = ret;
				break;
			}
		}
		if (origReturnBlock == null) {
			return;
		}
		for (BlockNode mb : merge) {
			if (mb == origReturnBlock) {
				continue;
			}
			for (BlockNode remPred : mb.getPredecessors()) {
				connect(remPred, origReturnBlock);
			}
			markForRemove(mb);
			edgeAttr.getBlocks().remove(mb);
		}
	}

	private static BlockNode getFinallyOutBlock(List<BlockNode> exitBlocks) {
		for (BlockNode exitBlock : exitBlocks) {
			for (BlockNode exitPred : exitBlock.getPredecessors()) {
				IgnoreEdgeAttr edgeAttr = exitPred.get(AType.IGNORE_EDGE);
				if (edgeAttr != null && edgeAttr.contains(exitBlock)) {
					return exitPred;
				}
			}
		}
		return null;
	}
}
