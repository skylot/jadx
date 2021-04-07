package jadx.core.dex.visitors.blocksmaker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.LoopInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.Edge;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlock;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.blocksmaker.BlockSplitter.connect;
import static jadx.core.utils.EmptyBitSet.EMPTY;

public class BlockProcessor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(BlockProcessor.class);

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		processBlocksTree(mth);
	}

	public static void rerun(MethodNode mth) {
		removeMarkedBlocks(mth);
		clearBlocksState(mth);
		processBlocksTree(mth);
	}

	private static void processBlocksTree(MethodNode mth) {
		computeDominators(mth);
		if (independentBlockTreeMod(mth)) {
			clearBlocksState(mth);
			computeDominators(mth);
		}
		updateExitBlocks(mth);

		int i = 0;
		while (modifyBlocksTree(mth)) {
			// revert calculations
			clearBlocksState(mth);
			// recalculate dominators tree
			computeDominators(mth);
			updateExitBlocks(mth);

			if (i++ > 100) {
				throw new JadxRuntimeException("CFG modification limit reached, blocks count: " + mth.getBasicBlocks().size());
			}
		}
		checkForUnreachableBlocks(mth);

		computeDominanceFrontier(mth);
		registerLoops(mth);
		processNestedLoops(mth);
	}

	private static void checkForUnreachableBlocks(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
				throw new JadxRuntimeException("Unreachable block: " + block);
			}
		});
	}

	private static boolean deduplicateBlockInsns(BlockNode block) {
		if (block.contains(AFlag.LOOP_START) || block.contains(AFlag.LOOP_END)) {
			// search for same instruction at end of all predecessors blocks
			List<BlockNode> predecessors = block.getPredecessors();
			int predsCount = predecessors.size();
			if (predsCount > 1) {
				InsnNode lastInsn = BlockUtils.getLastInsn(block);
				if (lastInsn != null && lastInsn.getType() == InsnType.IF) {
					return false;
				}
				// TODO: implement insn extraction into separate block for partial predecessors
				int sameInsnCount = getSameLastInsnCount(predecessors);
				if (sameInsnCount > 0) {
					List<InsnNode> insns = getLastInsns(predecessors.get(0), sameInsnCount);
					insertAtStart(block, insns);
					predecessors.forEach(pred -> getLastInsns(pred, sameInsnCount).clear());
					LOG.debug("Move duplicate insns, count: {} to block {}", sameInsnCount, block);
					return true;
				}
			}
		}
		return false;
	}

	private static List<InsnNode> getLastInsns(BlockNode blockNode, int sameInsnCount) {
		List<InsnNode> instructions = blockNode.getInstructions();
		int size = instructions.size();
		return instructions.subList(size - sameInsnCount, size);
	}

	private static void insertAtStart(BlockNode block, List<InsnNode> insns) {
		List<InsnNode> blockInsns = block.getInstructions();

		List<InsnNode> newInsnList = new ArrayList<>(insns.size() + blockInsns.size());
		newInsnList.addAll(insns);
		newInsnList.addAll(blockInsns);

		blockInsns.clear();
		blockInsns.addAll(newInsnList);
	}

	private static int getSameLastInsnCount(List<BlockNode> predecessors) {
		int sameInsnCount = 0;
		while (true) {
			InsnNode insn = null;
			for (BlockNode pred : predecessors) {
				InsnNode curInsn = getInsnsFromEnd(pred, sameInsnCount);
				if (curInsn == null) {
					return sameInsnCount;
				}
				if (insn == null) {
					insn = curInsn;
				} else {
					if (!isSame(insn, curInsn)) {
						return sameInsnCount;
					}
				}
			}
			sameInsnCount++;
		}
	}

	private static boolean isSame(InsnNode insn, InsnNode curInsn) {
		return isInsnsEquals(insn, curInsn) && insn.canReorder();
	}

	private static boolean isInsnsEquals(InsnNode insn, InsnNode otherInsn) {
		if (insn == otherInsn) {
			return true;
		}
		if (insn.isSame(otherInsn)
				&& sameArgs(insn.getResult(), otherInsn.getResult())) {
			int argsCount = insn.getArgsCount();
			for (int i = 0; i < argsCount; i++) {
				if (!sameArgs(insn.getArg(i), otherInsn.getArg(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	private static boolean sameArgs(@Nullable InsnArg arg, @Nullable InsnArg otherArg) {
		if (arg == otherArg) {
			return true;
		}
		if (arg == null || otherArg == null) {
			return false;
		}
		if (arg.getClass().equals(otherArg.getClass())) {
			if (arg.isRegister()) {
				return ((RegisterArg) arg).getRegNum() == ((RegisterArg) otherArg).getRegNum();
			}
			if (arg.isLiteral()) {
				return ((LiteralArg) arg).getLiteral() == ((LiteralArg) otherArg).getLiteral();
			}
			throw new JadxRuntimeException("Unexpected InsnArg types: " + arg + " and " + otherArg);
		}
		return false;
	}

	private static InsnNode getInsnsFromEnd(BlockNode block, int number) {
		List<InsnNode> instructions = block.getInstructions();
		int insnCount = instructions.size();
		if (insnCount <= number) {
			return null;
		}
		return instructions.get(insnCount - number - 1);
	}

	private static void computeDominators(MethodNode mth) {
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		int nBlocks = basicBlocks.size();
		for (int i = 0; i < nBlocks; i++) {
			BlockNode block = basicBlocks.get(i);
			block.setId(i);
			block.setDoms(new BitSet(nBlocks));
			block.getDoms().set(0, nBlocks);
		}

		BlockNode entryBlock = mth.getEnterBlock();
		calcDominators(basicBlocks, entryBlock);
		markLoops(mth);

		// clear self dominance
		basicBlocks.forEach(block -> {
			block.getDoms().clear(block.getId());
			if (block.getDoms().isEmpty()) {
				block.setDoms(EMPTY);
			}
		});

		calcImmediateDominators(basicBlocks, entryBlock);
	}

	private static void calcDominators(List<BlockNode> basicBlocks, BlockNode entryBlock) {
		entryBlock.getDoms().clear();
		entryBlock.getDoms().set(entryBlock.getId());

		BitSet domSet = new BitSet(basicBlocks.size());
		boolean changed;
		do {
			changed = false;
			for (BlockNode block : basicBlocks) {
				if (block == entryBlock) {
					continue;
				}
				BitSet d = block.getDoms();
				if (!changed) {
					domSet.clear();
					domSet.or(d);
				}
				for (BlockNode pred : block.getPredecessors()) {
					d.and(pred.getDoms());
				}
				d.set(block.getId());
				if (!changed && !d.equals(domSet)) {
					changed = true;
				}
			}
		} while (changed);
	}

	private static void calcImmediateDominators(List<BlockNode> basicBlocks, BlockNode entryBlock) {
		for (BlockNode block : basicBlocks) {
			if (block == entryBlock) {
				continue;
			}
			BlockNode idom;
			List<BlockNode> preds = block.getPredecessors();
			if (preds.size() == 1) {
				idom = preds.get(0);
			} else {
				BitSet bs = new BitSet(block.getDoms().length());
				bs.or(block.getDoms());
				for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
					BlockNode dom = basicBlocks.get(i);
					bs.andNot(dom.getDoms());
				}
				if (bs.cardinality() != 1) {
					throw new JadxRuntimeException("Can't find immediate dominator for block " + block
							+ " in " + bs + " preds:" + preds);
				}
				idom = basicBlocks.get(bs.nextSetBit(0));
			}
			block.setIDom(idom);
			idom.addDominatesOn(block);
		}
	}

	private static void computeDominanceFrontier(MethodNode mth) {
		for (BlockNode exit : mth.getExitBlocks()) {
			exit.setDomFrontier(EMPTY);
		}
		List<BlockNode> domSortedBlocks = new ArrayList<>(mth.getBasicBlocks().size());
		Deque<BlockNode> stack = new LinkedList<>();
		stack.push(mth.getEnterBlock());
		while (!stack.isEmpty()) {
			BlockNode node = stack.pop();
			for (BlockNode dominated : node.getDominatesOn()) {
				stack.push(dominated);
			}
			domSortedBlocks.add(node);
		}
		Collections.reverse(domSortedBlocks);
		for (BlockNode block : domSortedBlocks) {
			try {
				computeBlockDF(mth, block);
			} catch (Exception e) {
				throw new JadxRuntimeException("Failed compute block dominance frontier", e);
			}
		}
	}

	private static void computeBlockDF(MethodNode mth, BlockNode block) {
		if (block.getDomFrontier() != null) {
			return;
		}
		List<BlockNode> blocks = mth.getBasicBlocks();
		BitSet domFrontier = null;
		for (BlockNode s : block.getSuccessors()) {
			if (s.getIDom() != block) {
				if (domFrontier == null) {
					domFrontier = new BitSet(blocks.size());
				}
				domFrontier.set(s.getId());
			}
		}
		for (BlockNode c : block.getDominatesOn()) {
			BitSet frontier = c.getDomFrontier();
			if (frontier == null) {
				throw new JadxRuntimeException("Dominance frontier not calculated for dominated block: " + c + ", from: " + block);
			}
			for (int p = frontier.nextSetBit(0); p >= 0; p = frontier.nextSetBit(p + 1)) {
				if (blocks.get(p).getIDom() != block) {
					if (domFrontier == null) {
						domFrontier = new BitSet(blocks.size());
					}
					domFrontier.set(p);
				}
			}
		}
		if (domFrontier == null || domFrontier.isEmpty()) {
			domFrontier = EMPTY;
		}
		block.setDomFrontier(domFrontier);
	}

	private static void updateExitBlocks(MethodNode mth) {
		mth.getExitBlocks().clear();
		mth.getBasicBlocks().forEach(block -> {
			boolean noSuccessors = block.getSuccessors().isEmpty();
			boolean exitBlock = false;
			InsnNode lastInsn = BlockUtils.getLastInsn(block);
			if (lastInsn != null) {
				InsnType insnType = lastInsn.getType();
				if (insnType == InsnType.RETURN) {
					block.add(AFlag.RETURN);
					exitBlock = true;
					if (!noSuccessors) {
						throw new JadxRuntimeException("Found a block after RETURN instruction: " + lastInsn + " in block: " + block);
					}
				} else if (insnType == InsnType.THROW) {
					if (noSuccessors) {
						exitBlock = true;
					}
				}
			}
			if (exitBlock) {
				mth.addExitBlock(block);
			} else if (noSuccessors
					&& !mth.isVoidReturn()
					&& !mth.isConstructor()) {
				mth.addComment("JADX INFO: Unexpected exit block: " + block
						+ ". Expect last instruction to be RETURN or THROW, got: " + lastInsn);
			}
		});
	}

	private static void markLoops(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			// Every successor that dominates its predecessor is a header of a loop,
			// block -> successor is a back edge.
			block.getSuccessors().forEach(successor -> {
				if (block.getDoms().get(successor.getId())) {
					successor.add(AFlag.LOOP_START);
					block.add(AFlag.LOOP_END);

					LoopInfo loop = new LoopInfo(successor, block);
					successor.addAttr(AType.LOOP, loop);
					block.addAttr(AType.LOOP, loop);
				}
			});
		});
	}

	private static void registerLoops(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			if (block.contains(AFlag.LOOP_START)) {
				block.getAll(AType.LOOP).forEach(mth::registerLoop);
			}
		});
	}

	private static void processNestedLoops(MethodNode mth) {
		if (mth.getLoopsCount() == 0) {
			return;
		}
		for (LoopInfo outLoop : mth.getLoops()) {
			for (LoopInfo innerLoop : mth.getLoops()) {
				if (outLoop == innerLoop) {
					continue;
				}
				if (outLoop.getLoopBlocks().containsAll(innerLoop.getLoopBlocks())) {
					LoopInfo parentLoop = innerLoop.getParentLoop();
					if (parentLoop != null) {
						if (parentLoop.getLoopBlocks().containsAll(outLoop.getLoopBlocks())) {
							outLoop.setParentLoop(parentLoop);
							innerLoop.setParentLoop(outLoop);
						} else {
							parentLoop.setParentLoop(outLoop);
						}
					} else {
						innerLoop.setParentLoop(outLoop);
					}
				}
			}
		}
	}

	private static boolean modifyBlocksTree(MethodNode mth) {
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		for (BlockNode block : basicBlocks) {
			if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
				throw new JadxRuntimeException("Unreachable block: " + block);
			}
		}
		if (mergeExceptionHandlers(mth)) {
			removeMarkedBlocks(mth);
			return true;
		}
		for (BlockNode block : basicBlocks) {
			if (checkLoops(mth, block)) {
				return true;
			}
		}
		if (mergeConstReturn(mth)) {
			return true;
		}
		return splitReturnBlocks(mth);
	}

	private static boolean mergeConstReturn(MethodNode mth) {
		if (mth.isVoidReturn()) {
			return false;
		}

		boolean changed = false;
		for (BlockNode exitBlock : new ArrayList<>(mth.getExitBlocks())) {
			BlockNode pred = Utils.getOne(exitBlock.getPredecessors());
			if (pred != null) {
				InsnNode constInsn = Utils.getOne(pred.getInstructions());
				if (constInsn != null && constInsn.isConstInsn()) {
					RegisterArg constArg = constInsn.getResult();
					InsnNode returnInsn = BlockUtils.getLastInsn(exitBlock);
					if (returnInsn != null) {
						InsnArg retArg = returnInsn.getArg(0);
						if (constArg.sameReg(retArg)) {
							mergeConstAndReturnBlocks(mth, exitBlock, pred);
							changed = true;
						}
					}
				}
			}
		}
		if (changed) {
			removeMarkedBlocks(mth);
			cleanExitNodes(mth);
		}
		return changed;
	}

	private static void mergeConstAndReturnBlocks(MethodNode mth, BlockNode exitBlock, BlockNode pred) {
		pred.getInstructions().addAll(exitBlock.getInstructions());
		pred.copyAttributesFrom(exitBlock);
		BlockSplitter.removeConnection(pred, exitBlock);
		exitBlock.getInstructions().clear();
		exitBlock.add(AFlag.REMOVE);
	}

	private static boolean independentBlockTreeMod(MethodNode mth) {
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		boolean changed = false;
		for (BlockNode basicBlock : basicBlocks) {
			if (deduplicateBlockInsns(basicBlock)) {
				changed = true;
			}
		}
		for (BlockNode basicBlock : basicBlocks) {
			if (BlockSplitter.removeEmptyBlock(basicBlock)) {
				changed = true;
			}
		}
		if (BlockSplitter.removeEmptyDetachedBlocks(mth)) {
			changed = true;
		}
		return changed;
	}

	private static boolean checkLoops(MethodNode mth, BlockNode block) {
		if (!block.contains(AFlag.LOOP_START)) {
			return false;
		}
		List<LoopInfo> loops = block.getAll(AType.LOOP);
		int loopsCount = loops.size();
		if (loopsCount == 0) {
			return false;
		}
		for (LoopInfo loop : loops) {
			if (insertBlocksForBreak(mth, loop)) {
				return true;
			}
		}
		if (loopsCount > 1 && splitLoops(mth, block, loops)) {
			return true;
		}
		if (loopsCount == 1) {
			LoopInfo loop = loops.get(0);
			return insertBlocksForContinue(mth, loop)
					|| insertBlockForPredecessors(mth, loop)
					|| insertPreHeader(mth, loop);
		}
		return false;
	}

	/**
	 * Insert simple path block before loop header
	 */
	private static boolean insertPreHeader(MethodNode mth, LoopInfo loop) {
		BlockNode start = loop.getStart();
		List<BlockNode> preds = start.getPredecessors();
		int predsCount = preds.size() - 1; // don't count back edge
		if (predsCount == 1) {
			return false;
		}
		if (predsCount == 0) {
			if (!start.contains(AFlag.MTH_ENTER_BLOCK)) {
				mth.addWarnComment("Unexpected block without predecessors: " + start);
			}
			BlockNode newEnterBlock = BlockSplitter.startNewBlock(mth, -1);
			newEnterBlock.add(AFlag.SYNTHETIC);
			newEnterBlock.add(AFlag.MTH_ENTER_BLOCK);
			mth.setEnterBlock(newEnterBlock);
			start.remove(AFlag.MTH_ENTER_BLOCK);
			BlockSplitter.connect(newEnterBlock, start);
			return true;
		}
		// multiple predecessors
		BlockNode preHeader = BlockSplitter.startNewBlock(mth, -1);
		preHeader.add(AFlag.SYNTHETIC);
		for (BlockNode pred : new ArrayList<>(preds)) {
			BlockSplitter.replaceConnection(pred, start, preHeader);
		}
		BlockSplitter.connect(preHeader, start);
		return true;
	}

	/**
	 * Insert additional blocks for possible 'break' insertion
	 */
	private static boolean insertBlocksForBreak(MethodNode mth, LoopInfo loop) {
		boolean change = false;
		List<Edge> edges = loop.getExitEdges();
		if (!edges.isEmpty()) {
			for (Edge edge : edges) {
				BlockNode target = edge.getTarget();
				BlockNode source = edge.getSource();
				if (!target.contains(AFlag.SYNTHETIC) && !source.contains(AFlag.SYNTHETIC)) {
					BlockSplitter.insertBlockBetween(mth, source, target);
					change = true;
				}
			}
		}
		return change;
	}

	/**
	 * Insert additional blocks for possible 'continue' insertion
	 */
	private static boolean insertBlocksForContinue(MethodNode mth, LoopInfo loop) {
		BlockNode loopEnd = loop.getEnd();
		boolean change = false;
		List<BlockNode> preds = loopEnd.getPredecessors();
		if (preds.size() > 1) {
			for (BlockNode pred : new ArrayList<>(preds)) {
				if (!pred.contains(AFlag.SYNTHETIC)) {
					BlockSplitter.insertBlockBetween(mth, pred, loopEnd);
					change = true;
				}
			}
		}
		return change;
	}

	/**
	 * Insert additional block if loop header has several predecessors (exclude back edges)
	 */
	private static boolean insertBlockForPredecessors(MethodNode mth, LoopInfo loop) {
		BlockNode loopHeader = loop.getStart();
		List<BlockNode> preds = loopHeader.getPredecessors();
		if (preds.size() > 2) {
			List<BlockNode> blocks = new LinkedList<>(preds);
			blocks.removeIf(block -> block.contains(AFlag.LOOP_END));
			BlockNode first = blocks.remove(0);
			BlockNode preHeader = BlockSplitter.insertBlockBetween(mth, first, loopHeader);
			blocks.forEach(block -> BlockSplitter.replaceConnection(block, loopHeader, preHeader));
			return true;
		}
		return false;
	}

	private static boolean splitLoops(MethodNode mth, BlockNode block, List<LoopInfo> loops) {
		boolean oneHeader = true;
		for (LoopInfo loop : loops) {
			if (loop.getStart() != block) {
				oneHeader = false;
				break;
			}
		}
		if (oneHeader) {
			// several back edges connected to one loop header => make additional block
			BlockNode newLoopEnd = BlockSplitter.startNewBlock(mth, block.getStartOffset());
			newLoopEnd.add(AFlag.SYNTHETIC);
			connect(newLoopEnd, block);
			for (LoopInfo la : loops) {
				BlockSplitter.replaceConnection(la.getEnd(), block, newLoopEnd);
			}
			return true;
		}
		return false;
	}

	/**
	 * Merge handlers for multi-exception catch
	 */
	private static boolean mergeExceptionHandlers(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
			if (excHandlerAttr != null) {
				List<BlockNode> blocksForMerge = collectExcHandlerBlocks(block, excHandlerAttr);
				if (mergeHandlers(mth, blocksForMerge, excHandlerAttr)) {
					return true;
				}
			}
		}
		return false;
	}

	private static List<BlockNode> collectExcHandlerBlocks(BlockNode block, ExcHandlerAttr excHandlerAttr) {
		List<BlockNode> successors = block.getSuccessors();
		if (successors.size() != 1) {
			return Collections.emptyList();
		}
		RegisterArg reg = getMoveExceptionRegister(block);
		if (reg == null) {
			return Collections.emptyList();
		}
		TryCatchBlock tryBlock = excHandlerAttr.getTryBlock();
		List<BlockNode> blocksForMerge = new ArrayList<>();
		BlockNode nextBlock = successors.get(0);
		for (BlockNode predBlock : nextBlock.getPredecessors()) {
			if (predBlock != block
					&& checkOtherExcHandler(predBlock, tryBlock, reg)) {
				blocksForMerge.add(predBlock);
			}
		}
		return blocksForMerge;
	}

	private static boolean checkOtherExcHandler(BlockNode predBlock, TryCatchBlock tryBlock, RegisterArg reg) {
		ExcHandlerAttr otherExcHandlerAttr = predBlock.get(AType.EXC_HANDLER);
		if (otherExcHandlerAttr == null) {
			return false;
		}
		TryCatchBlock otherTryBlock = otherExcHandlerAttr.getTryBlock();
		if (tryBlock != otherTryBlock) {
			return false;
		}
		RegisterArg otherReg = getMoveExceptionRegister(predBlock);
		if (otherReg == null || reg.getRegNum() != otherReg.getRegNum()) {
			return false;
		}
		return true;
	}

	private static RegisterArg getMoveExceptionRegister(BlockNode block) {
		InsnNode insn = BlockUtils.getLastInsn(block);
		if (insn == null || insn.getType() != InsnType.MOVE_EXCEPTION) {
			return null;
		}
		return insn.getResult();
	}

	private static boolean mergeHandlers(MethodNode mth, List<BlockNode> blocksForMerge, ExcHandlerAttr excHandlerAttr) {
		if (blocksForMerge.isEmpty()) {
			return false;
		}
		TryCatchBlock tryBlock = excHandlerAttr.getTryBlock();
		for (BlockNode block : blocksForMerge) {
			ExcHandlerAttr otherExcHandlerAttr = block.get(AType.EXC_HANDLER);
			ExceptionHandler excHandler = otherExcHandlerAttr.getHandler();
			excHandlerAttr.getHandler().addCatchTypes(excHandler.getCatchTypes());
			tryBlock.removeHandler(mth, excHandler);
			BlockSplitter.detachBlock(block);
		}
		return true;
	}

	private static boolean splitReturnBlocks(MethodNode mth) {
		boolean changed = false;
		for (BlockNode exitBlock : mth.getExitBlocks()) {
			if (splitReturn(mth, exitBlock)) {
				changed = true;
			}
		}
		if (changed) {
			cleanExitNodes(mth);
		}
		return changed;
	}

	/**
	 * Splice return block if several predecessors presents
	 */
	private static boolean splitReturn(MethodNode mth, BlockNode exitBlock) {
		if (exitBlock.contains(AFlag.SYNTHETIC)
				|| exitBlock.contains(AFlag.ORIG_RETURN)
				|| exitBlock.contains(AType.SPLITTER_BLOCK)) {
			return false;
		}
		List<BlockNode> preds = exitBlock.getPredecessors();
		if (preds.size() < 2) {
			return false;
		}
		preds = BlockUtils.filterPredecessors(exitBlock);
		if (preds.size() < 2) {
			return false;
		}
		InsnNode returnInsn = BlockUtils.getLastInsn(exitBlock);
		if (returnInsn == null) {
			return false;
		}
		if (returnInsn.getArgsCount() == 1
				&& exitBlock.getInstructions().size() == 1
				&& !isReturnArgAssignInPred(preds, returnInsn)) {
			return false;
		}

		boolean first = true;
		for (BlockNode pred : preds) {
			BlockNode newRetBlock = BlockSplitter.startNewBlock(mth, -1);
			newRetBlock.add(AFlag.SYNTHETIC);
			if (first) {
				newRetBlock.add(AFlag.ORIG_RETURN);
				newRetBlock.getInstructions().addAll(exitBlock.getInstructions());
				first = false;
			} else {
				for (InsnNode oldInsn : exitBlock.getInstructions()) {
					InsnNode copyInsn = oldInsn.copyWithoutSsa();
					copyInsn.add(AFlag.SYNTHETIC);
					newRetBlock.getInstructions().add(copyInsn);
				}
			}
			BlockSplitter.replaceConnection(pred, exitBlock, newRetBlock);
		}
		return true;
	}

	private static boolean isReturnArgAssignInPred(List<BlockNode> preds, InsnNode returnInsn) {
		InsnArg retArg = returnInsn.getArg(0);
		if (retArg.isRegister()) {
			RegisterArg arg = (RegisterArg) retArg;
			int regNum = arg.getRegNum();
			for (BlockNode pred : preds) {
				for (InsnNode insnNode : pred.getInstructions()) {
					RegisterArg result = insnNode.getResult();
					if (result != null && result.getRegNum() == regNum) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private static void cleanExitNodes(MethodNode mth) {
		Iterator<BlockNode> iterator = mth.getExitBlocks().iterator();
		while (iterator.hasNext()) {
			BlockNode exitBlock = iterator.next();
			if (exitBlock.getPredecessors().isEmpty()) {
				mth.getBasicBlocks().remove(exitBlock);
				iterator.remove();
			}
		}
	}

	private static void removeMarkedBlocks(MethodNode mth) {
		mth.getBasicBlocks().removeIf(block -> {
			if (block.contains(AFlag.REMOVE)) {
				if (!block.getPredecessors().isEmpty() || !block.getSuccessors().isEmpty()) {
					LOG.warn("Block {} not deleted, method: {}", block, mth);
				} else {
					CatchAttr catchAttr = block.get(AType.CATCH_BLOCK);
					if (catchAttr != null) {
						catchAttr.getTryBlock().removeBlock(mth, block);
					}
					return true;
				}
			}
			return false;
		});
	}

	private static void clearBlocksState(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			block.remove(AType.LOOP);
			block.remove(AFlag.LOOP_START);
			block.remove(AFlag.LOOP_END);
			block.setDoms(null);
			block.setIDom(null);
			block.setDomFrontier(null);
			block.getDominatesOn().clear();
		});
	}
}
