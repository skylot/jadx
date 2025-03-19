package jadx.core.dex.visitors.blocks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
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
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static jadx.core.dex.visitors.blocks.BlockSplitter.connect;

public class BlockProcessor extends AbstractVisitor {
	private static final Logger LOG = LoggerFactory.getLogger(BlockProcessor.class);

	private static final boolean DEBUG_MODS = false;

	@Override
	public void visit(MethodNode mth) {
		if (mth.isNoCode() || mth.getBasicBlocks().isEmpty()) {
			return;
		}
		processBlocksTree(mth);
	}

	private static void processBlocksTree(MethodNode mth) {
		removeUnreachableBlocks(mth);

		computeDominators(mth);
		if (independentBlockTreeMod(mth)) {
			checkForUnreachableBlocks(mth);
			computeDominators(mth);
		}
		if (FixMultiEntryLoops.process(mth)) {
			computeDominators(mth);
		}
		updateCleanSuccessors(mth);

		int blocksCount = mth.getBasicBlocks().size();
		int modLimit = Math.max(100, blocksCount);
		if (DEBUG_MODS) {
			mth.addAttr(new DebugModAttr());
		}
		int i = 0;
		while (modifyBlocksTree(mth)) {
			computeDominators(mth);
			if (i++ > modLimit) {
				mth.addWarn("CFG modification limit reached, blocks count: " + blocksCount);
				break;
			}
		}
		if (DEBUG_MODS && i != 0) {
			String stats = "CFG modifications count: " + i
					+ ", blocks count: " + blocksCount + '\n'
					+ mth.get(DebugModAttr.TYPE).formatStats() + '\n';
			mth.addDebugComment(stats);
			LOG.debug("Method: {}\n{}", mth, stats);
			mth.remove(DebugModAttr.TYPE);
		}
		checkForUnreachableBlocks(mth);

		DominatorTree.computeDominanceFrontier(mth);
		registerLoops(mth);
		processNestedLoops(mth);

		PostDominatorTree.compute(mth);

		updateCleanSuccessors(mth);
	}

	/**
	 * Recalculate all additional info attached to blocks:
	 *
	 * <pre>
	 * - dominators
	 * - dominance frontier
	 * - post dominators (only if {@link AFlag#COMPUTE_POST_DOM} added to method)
	 * - loops and nested loop info
	 * </pre>
	 * <p>
	 * This method should be called after changing a block tree in custom passes added before
	 * {@link BlockFinisher}.
	 */
	public static void updateBlocksData(MethodNode mth) {
		clearBlocksState(mth);
		DominatorTree.compute(mth);
		markLoops(mth);

		DominatorTree.computeDominanceFrontier(mth);
		registerLoops(mth);
		processNestedLoops(mth);

		PostDominatorTree.compute(mth);

		updateCleanSuccessors(mth);
	}

	static void updateCleanSuccessors(MethodNode mth) {
		mth.getBasicBlocks().forEach(BlockNode::updateCleanSuccessors);
	}

	private static void checkForUnreachableBlocks(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
				throw new JadxRuntimeException("Unreachable block: " + block);
			}
		}
	}

	private static boolean deduplicateBlockInsns(MethodNode mth, BlockNode block) {
		if (block.contains(AFlag.LOOP_START) || block.contains(AFlag.LOOP_END)) {
			// search for same instruction at end of all predecessors blocks
			List<BlockNode> predecessors = block.getPredecessors();
			int predsCount = predecessors.size();
			if (predsCount > 1) {
				InsnNode lastInsn = BlockUtils.getLastInsn(block);
				if (lastInsn != null && lastInsn.getType() == InsnType.IF) {
					return false;
				}
				if (BlockUtils.checkFirstInsn(block, insn -> insn.contains(AType.EXC_HANDLER))) {
					return false;
				}
				// TODO: implement insn extraction into separate block for partial predecessors
				int sameInsnCount = getSameLastInsnCount(predecessors);
				if (sameInsnCount > 0) {
					List<InsnNode> insns = getLastInsns(predecessors.get(0), sameInsnCount);
					insertAtStart(block, insns);
					predecessors.forEach(pred -> getLastInsns(pred, sameInsnCount).clear());
					mth.addDebugComment("Move duplicate insns, count: " + sameInsnCount + " to block " + block);
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
		clearBlocksState(mth);
		DominatorTree.compute(mth);
		markLoops(mth);
	}

	private static void markLoops(MethodNode mth) {
		mth.getBasicBlocks().forEach(block -> {
			// Every successor that dominates its predecessor is a header of a loop,
			// block -> successor is a back edge.
			block.getSuccessors().forEach(successor -> {
				if (block.getDoms().get(successor.getId()) || block == successor) {
					successor.add(AFlag.LOOP_START);
					block.add(AFlag.LOOP_END);

					Set<BlockNode> loopBlocks = BlockUtils.getAllPathsBlocks(successor, block);
					LoopInfo loop = new LoopInfo(successor, block, loopBlocks);
					successor.addAttr(AType.LOOP, loop);
					block.addAttr(AType.LOOP, loop);
				}
			});
		});
	}

	private static void registerLoops(MethodNode mth) {
		mth.resetLoops();
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
		for (BlockNode block : mth.getBasicBlocks()) {
			if (checkLoops(mth, block)) {
				return true;
			}
		}
		if (mergeConstReturn(mth)) {
			return true;
		}
		return splitExitBlocks(mth);
	}

	private static boolean mergeConstReturn(MethodNode mth) {
		if (mth.isVoidReturn()) {
			return false;
		}
		boolean changed = false;
		for (BlockNode retBlock : new ArrayList<>(mth.getPreExitBlocks())) {
			BlockNode pred = Utils.getOne(retBlock.getPredecessors());
			if (pred != null) {
				InsnNode constInsn = Utils.getOne(pred.getInstructions());
				if (constInsn != null && constInsn.isConstInsn()) {
					RegisterArg constArg = constInsn.getResult();
					InsnNode returnInsn = BlockUtils.getLastInsn(retBlock);
					if (returnInsn != null && returnInsn.getType() == InsnType.RETURN) {
						InsnArg retArg = returnInsn.getArg(0);
						if (constArg.sameReg(retArg)) {
							mergeConstAndReturnBlocks(mth, retBlock, pred);
							changed = true;
						}
					}
				}
			}
		}
		if (changed) {
			removeMarkedBlocks(mth);
			if (DEBUG_MODS) {
				mth.get(DebugModAttr.TYPE).addEvent("Merge const return");
			}
		}
		return changed;
	}

	private static void mergeConstAndReturnBlocks(MethodNode mth, BlockNode retBlock, BlockNode pred) {
		pred.getInstructions().addAll(retBlock.getInstructions());
		pred.copyAttributesFrom(retBlock);
		BlockSplitter.removeConnection(pred, retBlock);
		retBlock.getInstructions().clear();
		retBlock.add(AFlag.REMOVE);
		BlockNode exitBlock = mth.getExitBlock();
		BlockSplitter.removeConnection(retBlock, exitBlock);
		BlockSplitter.connect(pred, exitBlock);
		pred.updateCleanSuccessors();
	}

	private static boolean independentBlockTreeMod(MethodNode mth) {
		boolean changed = false;
		List<BlockNode> basicBlocks = mth.getBasicBlocks();
		for (BlockNode basicBlock : basicBlocks) {
			if (deduplicateBlockInsns(mth, basicBlock)) {
				changed = true;
			}
		}
		if (BlockExceptionHandler.process(mth)) {
			changed = true;
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

	private static boolean simplifyLoopEnd(MethodNode mth, LoopInfo loop) {
		BlockNode loopEnd = loop.getEnd();
		if (loopEnd.getSuccessors().size() <= 1) {
			return false;
		}
		// make loop end a simple path block
		BlockNode newLoopEnd = BlockSplitter.startNewBlock(mth, -1);
		newLoopEnd.add(AFlag.SYNTHETIC);
		newLoopEnd.add(AFlag.LOOP_END);
		BlockNode loopStart = loop.getStart();
		BlockSplitter.replaceConnection(loopEnd, loopStart, newLoopEnd);
		BlockSplitter.connect(newLoopEnd, loopStart);
		if (DEBUG_MODS) {
			mth.get(DebugModAttr.TYPE).addEvent("Simplify loop end");
		}
		return true;
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
					|| insertPreHeader(mth, loop)
					|| simplifyLoopEnd(mth, loop);
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
		} else {
			// multiple predecessors
			BlockNode preHeader = BlockSplitter.startNewBlock(mth, -1);
			preHeader.add(AFlag.SYNTHETIC);
			BlockNode loopEnd = loop.getEnd();
			for (BlockNode pred : new ArrayList<>(preds)) {
				if (pred != loopEnd) {
					BlockSplitter.replaceConnection(pred, start, preHeader);
				}
			}
			BlockSplitter.connect(preHeader, start);
		}
		if (DEBUG_MODS) {
			mth.get(DebugModAttr.TYPE).addEvent("Insert loop pre header");
		}
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
		if (DEBUG_MODS && change) {
			mth.get(DebugModAttr.TYPE).addEvent("Insert loop break blocks");
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
		if (DEBUG_MODS && change) {
			mth.get(DebugModAttr.TYPE).addEvent("Insert loop continue block");
		}
		return change;
	}

	private static boolean splitLoops(MethodNode mth, BlockNode block, List<LoopInfo> loops) {
		boolean oneHeader = true;
		for (LoopInfo loop : loops) {
			if (loop.getStart() != block) {
				oneHeader = false;
				break;
			}
		}
		if (!oneHeader) {
			return false;
		}
		// several back edges connected to one loop header => make additional block
		BlockNode newLoopEnd = BlockSplitter.startNewBlock(mth, block.getStartOffset());
		newLoopEnd.add(AFlag.SYNTHETIC);
		connect(newLoopEnd, block);
		for (LoopInfo la : loops) {
			BlockSplitter.replaceConnection(la.getEnd(), block, newLoopEnd);
		}
		if (DEBUG_MODS) {
			mth.get(DebugModAttr.TYPE).addEvent("Split loops");
		}
		return true;
	}

	private static boolean splitExitBlocks(MethodNode mth) {
		boolean changed = false;
		for (BlockNode preExitBlock : mth.getPreExitBlocks()) {
			if (splitReturn(mth, preExitBlock)) {
				changed = true;
			} else if (splitThrow(mth, preExitBlock)) {
				changed = true;
			}
		}
		if (changed) {
			updateExitBlockConnections(mth);
			if (DEBUG_MODS) {
				mth.get(DebugModAttr.TYPE).addEvent("Split exit block");
			}
		}
		return changed;
	}

	private static void updateExitBlockConnections(MethodNode mth) {
		BlockNode exitBlock = mth.getExitBlock();
		BlockSplitter.removePredecessors(exitBlock);
		for (BlockNode block : mth.getBasicBlocks()) {
			if (block != exitBlock
					&& block.getSuccessors().isEmpty()
					&& !block.contains(AFlag.REMOVE)) {
				BlockSplitter.connect(block, exitBlock);
			}
		}
	}

	/**
	 * Splice return block if several predecessors presents
	 */
	private static boolean splitReturn(MethodNode mth, BlockNode returnBlock) {
		if (returnBlock.contains(AFlag.SYNTHETIC)
				|| returnBlock.contains(AFlag.ORIG_RETURN)
				|| returnBlock.contains(AType.EXC_HANDLER)) {
			return false;
		}
		List<BlockNode> preds = returnBlock.getPredecessors();
		if (preds.size() < 2) {
			return false;
		}
		InsnNode returnInsn = BlockUtils.getLastInsn(returnBlock);
		if (returnInsn == null) {
			return false;
		}
		if (returnInsn.getArgsCount() == 1
				&& returnBlock.getInstructions().size() == 1
				&& !isArgAssignInPred(preds, returnInsn.getArg(0))) {
			return false;
		}

		boolean first = true;
		for (BlockNode pred : new ArrayList<>(preds)) {
			if (first) {
				returnBlock.add(AFlag.ORIG_RETURN);
				first = false;
			} else {
				BlockNode newRetBlock = BlockSplitter.startNewBlock(mth, -1);
				newRetBlock.add(AFlag.SYNTHETIC);
				newRetBlock.add(AFlag.RETURN);
				for (InsnNode oldInsn : returnBlock.getInstructions()) {
					InsnNode copyInsn = oldInsn.copyWithoutSsa();
					copyInsn.add(AFlag.SYNTHETIC);
					newRetBlock.getInstructions().add(copyInsn);
				}
				BlockSplitter.replaceConnection(pred, returnBlock, newRetBlock);
			}
		}
		return true;
	}

	private static boolean splitThrow(MethodNode mth, BlockNode exitBlock) {
		if (exitBlock.contains(AFlag.IGNORE_THROW_SPLIT)) {
			return false;
		}
		List<BlockNode> preds = exitBlock.getPredecessors();
		if (preds.size() < 2) {
			return false;
		}
		InsnNode throwInsn = BlockUtils.getLastInsn(exitBlock);
		if (throwInsn == null || throwInsn.getType() != InsnType.THROW) {
			return false;
		}
		// split only for several exception handlers
		// traverse predecessors to exception handler
		Map<BlockNode, ExcHandlerAttr> handlersMap = new HashMap<>(preds.size());
		Set<BlockNode> handlers = new HashSet<>(preds.size());
		for (BlockNode pred : preds) {
			BlockUtils.visitPredecessorsUntil(mth, pred, block -> {
				ExcHandlerAttr excHandlerAttr = block.get(AType.EXC_HANDLER);
				if (excHandlerAttr == null) {
					return false;
				}
				boolean correctHandler = excHandlerAttr.getHandler().getBlocks().contains(block);
				if (correctHandler && isArgAssignInPred(Collections.singletonList(block), throwInsn.getArg(0))) {
					handlersMap.put(pred, excHandlerAttr);
					handlers.add(block);
				}
				return correctHandler;
			});
		}
		if (handlers.size() == 1) {
			exitBlock.add(AFlag.IGNORE_THROW_SPLIT);
			return false;
		}

		boolean first = true;
		for (BlockNode pred : new ArrayList<>(preds)) {
			if (first) {
				first = false;
			} else {
				BlockNode newThrowBlock = BlockSplitter.startNewBlock(mth, -1);
				newThrowBlock.add(AFlag.SYNTHETIC);
				for (InsnNode oldInsn : exitBlock.getInstructions()) {
					InsnNode copyInsn = oldInsn.copyWithoutSsa();
					copyInsn.add(AFlag.SYNTHETIC);
					newThrowBlock.getInstructions().add(copyInsn);
				}
				newThrowBlock.copyAttributesFrom(exitBlock);
				ExcHandlerAttr excHandlerAttr = handlersMap.get(pred);
				if (excHandlerAttr != null) {
					excHandlerAttr.getHandler().addBlock(newThrowBlock);
				}
				BlockSplitter.replaceConnection(pred, exitBlock, newThrowBlock);
			}
		}
		return true;
	}

	private static boolean isArgAssignInPred(List<BlockNode> preds, InsnArg arg) {
		if (arg.isRegister()) {
			int regNum = ((RegisterArg) arg).getRegNum();
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

	public static void removeMarkedBlocks(MethodNode mth) {
		boolean removed = mth.getBasicBlocks().removeIf(block -> {
			if (block.contains(AFlag.REMOVE)) {
				if (!block.getPredecessors().isEmpty() || !block.getSuccessors().isEmpty()) {
					LOG.warn("Block {} not deleted, method: {}", block, mth);
				} else {
					TryCatchBlockAttr tryBlockAttr = block.get(AType.TRY_BLOCK);
					if (tryBlockAttr != null) {
						tryBlockAttr.removeBlock(block);
					}
					return true;
				}
			}
			return false;
		});
		if (removed) {
			mth.updateBlockPositions();
		}
	}

	private static void removeUnreachableBlocks(MethodNode mth) {
		Set<BlockNode> toRemove = new LinkedHashSet<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			computeUnreachableFromBlock(toRemove, block, mth);
		}
		removeFromMethod(toRemove, mth);
	}

	public static void removeUnreachableBlock(BlockNode blockToRemove, MethodNode mth) {
		Set<BlockNode> toRemove = new LinkedHashSet<>();
		computeUnreachableFromBlock(toRemove, blockToRemove, mth);
		removeFromMethod(toRemove, mth);
	}

	private static void computeUnreachableFromBlock(Set<BlockNode> toRemove, BlockNode block, MethodNode mth) {
		if (block.getPredecessors().isEmpty() && block != mth.getEnterBlock()) {
			BlockSplitter.collectSuccessors(block, mth.getEnterBlock(), toRemove);
		}
	}

	private static void removeFromMethod(Set<BlockNode> toRemove, MethodNode mth) {
		if (toRemove.isEmpty()) {
			return;
		}

		long notEmptyBlocks = toRemove.stream().filter(block -> !block.getInstructions().isEmpty()).count();
		if (notEmptyBlocks != 0) {
			int insnsCount = toRemove.stream().mapToInt(block -> block.getInstructions().size()).sum();
			mth.addWarnComment("Unreachable blocks removed: " + notEmptyBlocks + ", instructions: " + insnsCount);
		}

		toRemove.forEach(BlockSplitter::detachBlock);
		mth.getBasicBlocks().removeAll(toRemove);
		mth.updateBlockPositions();
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

	private static final class DebugModAttr implements IJadxAttribute {
		static final IJadxAttrType<DebugModAttr> TYPE = IJadxAttrType.create("DebugModAttr");

		private final Map<String, Integer> statMap = new HashMap<>();

		public void addEvent(String name) {
			statMap.merge(name, 1, Integer::sum);
		}

		public String formatStats() {
			return statMap.entrySet().stream()
					.map(entry -> " " + entry.getKey() + ": " + entry.getValue())
					.collect(Collectors.joining("\n"));
		}

		@Override
		public IJadxAttrType<DebugModAttr> getAttrType() {
			return TYPE;
		}
	}
}
