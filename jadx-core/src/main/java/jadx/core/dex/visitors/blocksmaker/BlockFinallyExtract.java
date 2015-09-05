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
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.SplitterBlockAttr;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.blocksmaker.helpers.BlocksPair;
import jadx.core.dex.visitors.blocksmaker.helpers.BlocksRemoveInfo;
import jadx.core.dex.visitors.ssa.LiveVarAnalysis;
import jadx.core.utils.BlockUtils;
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
		for (ExceptionHandler excHandler : mth.getExceptionHandlers()) {
			if (processExceptionHandler(mth, excHandler)) {
				reloadBlocks = true;
			}
		}
		if (reloadBlocks) {
			mergeReturnBlocks(mth);
			BlockProcessor.rerun(mth);
		}
	}

	private static boolean processExceptionHandler(MethodNode mth, ExceptionHandler excHandler) {
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
				insns.remove(size - 1);
			}
		}
		if (reThrowRemoved && noExitNode
				&& extractFinally(mth, excHandler)) {
			return true;
		}
		int totalSize = countInstructions(excHandler);
		if (totalSize == 0 && reThrowRemoved && noExitNode) {
			excHandler.getTryBlock().removeHandler(mth, excHandler);
		}
		return false;
	}

	/**
	 * Search and remove common code from 'catch' and 'handlers'.
	 */
	private static boolean extractFinally(MethodNode mth, ExceptionHandler handler) {
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

		// 'finally' extract confirmed, run remove steps

		LiveVarAnalysis laBefore = null;
		boolean runReMap = isReMapNeeded(removes);
		if (runReMap) {
			laBefore = new LiveVarAnalysis(mth);
			laBefore.runAnalysis();
		}

		for (BlocksRemoveInfo removeInfo : removes) {
			if (!applyRemove(mth, removeInfo)) {
				return false;
			}
		}

		LiveVarAnalysis laAfter = null;

		// remove 'move-exception' instruction
		BlockNode handlerBlock = handler.getHandlerBlock();
		InsnNode me = BlockUtils.getLastInsn(handlerBlock);
		if (me != null && me.getType() == InsnType.MOVE_EXCEPTION) {
			boolean replaced = false;
			List<InsnNode> insnsList = handlerBlock.getInstructions();
			if (!handlerBlock.getCleanSuccessors().isEmpty()) {
				laAfter = new LiveVarAnalysis(mth);
				laAfter.runAnalysis();

				RegisterArg resArg = me.getResult();
				BlockNode succ = handlerBlock.getCleanSuccessors().get(0);
				if (laAfter.isLive(succ, resArg.getRegNum())) {
					// kill variable
					InsnNode kill = new InsnNode(InsnType.NOP, 0);
					kill.setResult(resArg);
					kill.add(AFlag.REMOVE);
					insnsList.set(insnsList.size() - 1, kill);
					replaced = true;
				}
			}
			if (!replaced) {
				insnsList.remove(insnsList.size() - 1);
				handlerBlock.add(AFlag.SKIP);
			}
		}

		// generate 'move' instruction for mapped register pairs
		if (runReMap) {
			if (laAfter == null) {
				laAfter = new LiveVarAnalysis(mth);
				laAfter.runAnalysis();
			}
			performVariablesReMap(mth, removes, laBefore, laAfter);
		}

		handler.setFinally(true);
		return true;
	}

	private static void performVariablesReMap(MethodNode mth, List<BlocksRemoveInfo> removes,
			LiveVarAnalysis laBefore, LiveVarAnalysis laAfter) {
		BitSet processed = new BitSet(mth.getRegsCount());
		for (BlocksRemoveInfo removeInfo : removes) {
			processed.clear();
			BlocksPair start = removeInfo.getStart();
			BlockNode insertBlockBefore = start.getFirst();
			BlockNode insertBlock = start.getSecond();
			if (removeInfo.getRegMap().isEmpty() || insertBlock == null) {
				continue;
			}
			for (Map.Entry<RegisterArg, RegisterArg> entry : removeInfo.getRegMap().entrySet()) {
				RegisterArg fromReg = entry.getKey();
				RegisterArg toReg = entry.getValue();
				int fromRegNum = fromReg.getRegNum();
				int toRegNum = toReg.getRegNum();
				if (!processed.get(fromRegNum)) {
					boolean liveFromBefore = laBefore.isLive(insertBlockBefore, fromRegNum);
					boolean liveFromAfter = laAfter.isLive(insertBlock, fromRegNum);
					// boolean liveToBefore = laBefore.isLive(insertBlock, toRegNum);
					boolean liveToAfter = laAfter.isLive(insertBlock, toRegNum);
					if (liveToAfter && liveFromBefore) {
						// merge 'to' and 'from' registers
						InsnNode merge = new InsnNode(InsnType.MERGE, 2);
						merge.setResult(toReg.duplicate());
						merge.addArg(toReg.duplicate());
						merge.addArg(fromReg.duplicate());
						injectInsn(mth, insertBlock, merge);
					} else if (liveFromBefore) {
						// remap variable
						InsnNode move = new InsnNode(InsnType.MOVE, 1);
						move.setResult(toReg.duplicate());
						move.addArg(fromReg.duplicate());
						injectInsn(mth, insertBlock, move);
					} else if (liveFromAfter) {
						// kill variable
						InsnNode kill = new InsnNode(InsnType.NOP, 0);
						kill.setResult(fromReg.duplicate());
						kill.add(AFlag.REMOVE);
						injectInsn(mth, insertBlock, kill);
					}
					processed.set(fromRegNum);
				}
			}
		}
	}

	private static void injectInsn(MethodNode mth, BlockNode insertBlock, InsnNode insn) {
		insn.add(AFlag.SYNTHETIC);
		if (insertBlock.getInstructions().isEmpty()) {
			insertBlock.getInstructions().add(insn);
		} else {
			BlockNode succBlock = splitBlock(mth, insertBlock, 0);
			BlockNode predBlock = succBlock.getPredecessors().get(0);
			predBlock.getInstructions().add(insn);
		}
	}

	private static boolean isReMapNeeded(List<BlocksRemoveInfo> removes) {
		for (BlocksRemoveInfo removeInfo : removes) {
			if (!removeInfo.getRegMap().isEmpty()) {
				return true;
			}
		}
		return false;
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
		Set<BlocksPair> outs = removeInfo.getOuts();
		if (outs.size() == 1) {
			return removeInfo;
		}
		// check if several 'return' blocks maps to one out
		if (mergeReturns(mth, outs)) {
			return removeInfo;
		}
		LOG.debug("Unexpected finally block outs count: {}", outs);
		return null;
	}

	private static boolean mergeReturns(MethodNode mth, Set<BlocksPair> outs) {
		Set<BlockNode> rightOuts = new HashSet<BlockNode>();
		boolean allReturns = true;
		for (BlocksPair outPair : outs) {
			BlockNode first = outPair.getFirst();
			if (!first.isReturnBlock()) {
				allReturns = false;
			}
			rightOuts.add(outPair.getSecond());
		}
		if (!allReturns || rightOuts.size() != 1) {
			return false;
		}
		Iterator<BlocksPair> it = outs.iterator();
		while (it.hasNext()) {
			BlocksPair out = it.next();
			BlockNode returnBlock = out.getFirst();
			if (!returnBlock.contains(AFlag.ORIG_RETURN)) {
				markForRemove(mth, returnBlock);
				it.remove();
			}
		}
		return true;
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
		int startPos = remInsns.size() - startInsns.size();
		int endPos = 0;
		if (!checkInsns(remInsns, startInsns, startPos, null)) {
			if (checkInsns(remInsns, startInsns, 0, null)) {
				startPos = 0;
				endPos = startInsns.size();
			} else {
				boolean found = false;
				for (int i = 1; i < startPos; i++) {
					if (checkInsns(remInsns, startInsns, i, null)) {
						startPos = i;
						endPos = startInsns.size() + i;
						found = true;
						break;
					}
				}
				if (!found) {
					return null;
				}
			}
		}
		BlocksPair startPair = new BlocksPair(remBlock, startBlock);
		BlocksRemoveInfo removeInfo = new BlocksRemoveInfo(startPair);
		removeInfo.setStartSplitIndex(startPos);
		removeInfo.setEndSplitIndex(endPos);
		if (endPos != 0) {
			removeInfo.setEnd(startPair);
		}
		// second - run checks again for collect registers mapping
		if (!checkInsns(remInsns, startInsns, startPos, removeInfo)) {
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
		BlocksPair currentPair = new BlocksPair(remBlock, startBlock);
		removeInfo.getProcessed().add(currentPair);

		List<BlockNode> baseCS = startBlock.getCleanSuccessors();
		List<BlockNode> remCS = remBlock.getCleanSuccessors();
		if (baseCS.size() != remCS.size()) {
			removeInfo.getOuts().add(currentPair);
			return true;
		}
		for (int i = 0; i < baseCS.size(); i++) {
			BlockNode sBlock = baseCS.get(i);
			BlockNode rBlock = remCS.get(i);
			if (bs.get(sBlock.getId())) {
				if (removeInfo.getEndSplitIndex() != 0) {
					// end block is not correct
					return false;
				}
				if (!checkBlocksTree(rBlock, sBlock, removeInfo, bs)) {
					return false;
				}
			} else {
				removeInfo.getOuts().add(new BlocksPair(rBlock, sBlock));
			}
		}
		return true;
	}

	private static boolean sameBlocks(BlockNode remBlock, BlockNode finallyBlock, BlocksRemoveInfo removeInfo) {
		List<InsnNode> first = remBlock.getInstructions();
		List<InsnNode> second = finallyBlock.getInstructions();
		if (first.size() < second.size()) {
			return false;
		}
		int size = second.size();
		for (int i = 0; i < size; i++) {
			if (!sameInsns(first.get(i), second.get(i), removeInfo)) {
				return false;
			}
		}
		if (first.size() > second.size()) {
			removeInfo.setEndSplitIndex(second.size());
			removeInfo.setEnd(new BlocksPair(remBlock, finallyBlock));
		}
		return true;
	}

	private static boolean sameInsns(InsnNode remInsn, InsnNode fInsn, BlocksRemoveInfo removeInfo) {
		if (!remInsn.isSame(fInsn)) {
			return false;
		}
		// TODO: check instance arg in ConstructorInsn
		// TODO: compare literals
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
			LOG.warn("Finally extract failed: remBlock pred: {}, {}, method: {}", remBlock, remBlock.getPredecessors(), mth);
			return false;
		}

		BlockNode remBlockPred = remBlock.getPredecessors().get(0);
		removeInfo.setStartPredecessor(remBlockPred);

		int startSplitIndex = removeInfo.getStartSplitIndex();
		int endSplitIndex = removeInfo.getEndSplitIndex();
		if (removeInfo.getStart().equals(removeInfo.getEnd())) {
			removeInfo.setEndSplitIndex(endSplitIndex - startSplitIndex);
		}
		// split start block (remBlock)
		if (startSplitIndex > 0) {
			remBlock = splitBlock(mth, remBlock, startSplitIndex);
			// change start block in removeInfo
			removeInfo.getProcessed().remove(removeInfo.getStart());
			BlocksPair newStart = new BlocksPair(remBlock, startBlock);
//			removeInfo.setStart(newStart);
			removeInfo.getProcessed().add(newStart);
		}
		// split end block
		if (endSplitIndex > 0) {
			BlocksPair end = removeInfo.getEnd();
			BlockNode newOut = splitBlock(mth, end.getFirst(), endSplitIndex);
			for (BlockNode s : newOut.getSuccessors()) {
				BlocksPair replaceOut = null;
				Iterator<BlocksPair> it = removeInfo.getOuts().iterator();
				while (it.hasNext()) {
					BlocksPair outPair = it.next();
					if (outPair.getFirst().equals(s)) {
						it.remove();
						replaceOut = new BlocksPair(newOut, outPair.getSecond());
						break;
					}
				}
				if (replaceOut != null) {
					removeInfo.getOuts().add(replaceOut);
				}
			}
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
				LOG.error("Block not found by {}, in {}, method: {}", pred, removeInfo, mth);
				return false;
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
			BlockNode middle = insertBlockBetween(mth, pred, remBlock);
			removeConnection(middle, remBlock);
			connect(middle, startBlock);
			addIgnoredEdge(middle, startBlock);
			connect(middle, rOut);
		}

		// mark blocks for remove
		markForRemove(mth, remBlock);
		for (BlocksPair pair : removeInfo.getProcessed()) {
			markForRemove(mth, pair.getFirst());
			BlockNode second = pair.getSecond();
			second.updateCleanSuccessors();
		}
		return true;
	}

	/**
	 * Split one block into connected 2 blocks with same connections.
	 *
	 * @return new successor block
	 */
	private static BlockNode splitBlock(MethodNode mth, BlockNode block, int splitIndex) {
		BlockNode newBlock = BlockSplitter.startNewBlock(mth, -1);

		newBlock.getSuccessors().addAll(block.getSuccessors());
		for (BlockNode s : new ArrayList<BlockNode>(block.getSuccessors())) {
			removeConnection(block, s);
			connect(newBlock, s);
		}
		block.getSuccessors().clear();
		connect(block, newBlock);
		block.updateCleanSuccessors();
		newBlock.updateCleanSuccessors();

		List<InsnNode> insns = block.getInstructions();
		int size = insns.size();
		for (int i = splitIndex; i < size; i++) {
			InsnNode insnNode = insns.get(i);
			insnNode.add(AFlag.SKIP);
			newBlock.getInstructions().add(insnNode);
		}
		Iterator<InsnNode> it = insns.iterator();
		while (it.hasNext()) {
			InsnNode insnNode = it.next();
			if (insnNode.contains(AFlag.SKIP)) {
				it.remove();
			}
		}
		for (InsnNode insnNode : newBlock.getInstructions()) {
			insnNode.remove(AFlag.SKIP);
		}
		return newBlock;
	}

	/**
	 * Unbind block for removing.
	 */
	private static void markForRemove(MethodNode mth, BlockNode block) {
		for (BlockNode p : block.getPredecessors()) {
			p.getSuccessors().remove(block);
			p.updateCleanSuccessors();
		}
		for (BlockNode s : block.getSuccessors()) {
			s.getPredecessors().remove(block);
		}
		block.getPredecessors().clear();
		block.getSuccessors().clear();
		block.add(AFlag.REMOVE);
		block.remove(AFlag.SKIP);

		CatchAttr catchAttr = block.get(AType.CATCH_BLOCK);
		if (catchAttr != null) {
			catchAttr.getTryBlock().removeBlock(mth, block);
			for (BlockNode skipBlock : mth.getBasicBlocks()) {
				if (skipBlock.contains(AFlag.SKIP)) {
					markForRemove(mth, skipBlock);
				}
			}
		}
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
			markForRemove(mth, mb);
			edgeAttr.getBlocks().remove(mb);
		}
		mergeSyntheticPredecessors(mth, origReturnBlock);
	}

	private static void mergeSyntheticPredecessors(MethodNode mth, BlockNode block) {
		List<BlockNode> preds = new ArrayList<BlockNode>(block.getPredecessors());
		Iterator<BlockNode> it = preds.iterator();
		while (it.hasNext()) {
			BlockNode predBlock = it.next();
			if (!predBlock.isSynthetic()) {
				it.remove();
			}
		}
		if (preds.size() < 2) {
			return;
		}
		BlockNode commonBlock = null;
		for (BlockNode predBlock : preds) {
			List<BlockNode> successors = predBlock.getSuccessors();
			if (successors.size() != 2) {
				return;
			}
			BlockNode cmnBlk = BlockUtils.selectOtherSafe(block, successors);
			if (commonBlock == null) {
				commonBlock = cmnBlk;
			} else if (cmnBlk != commonBlock) {
				return;
			}
			if (!predBlock.contains(AType.IGNORE_EDGE)) {
				return;
			}
		}
		if (commonBlock == null) {
			return;
		}

		// merge confirmed
		BlockNode mergeBlock = null;
		for (BlockNode predBlock : preds) {
			if (mergeBlock == null) {
				mergeBlock = predBlock;
				continue;
			}
			for (BlockNode remPred : predBlock.getPredecessors()) {
				connect(remPred, mergeBlock);
			}
			markForRemove(mth, predBlock);
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
