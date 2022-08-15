package jadx.core.dex.visitors.blocks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.utils.Utils;
import jadx.core.Consts;
import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.TmpEdgeAttr;
import jadx.core.dex.info.ClassInfo;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.NamedArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.CatchAttr;
import jadx.core.dex.trycatch.ExcHandlerAttr;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.dex.visitors.typeinference.TypeCompare;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.InsnRemover;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class BlockExceptionHandler {
	private static final Logger LOG = LoggerFactory.getLogger(BlockExceptionHandler.class);

	public static boolean process(MethodNode mth) {
		if (mth.isNoExceptionHandlers()) {
			return false;
		}
		BlockProcessor.updateCleanSuccessors(mth);
		DominatorTree.computeDominanceFrontier(mth);

		processCatchAttr(mth);
		initExcHandlers(mth);

		List<TryCatchBlockAttr> tryBlocks = prepareTryBlocks(mth);
		connectExcHandlers(mth, tryBlocks);
		mth.addAttr(AType.TRY_BLOCKS_LIST, tryBlocks);
		mth.getBasicBlocks().forEach(BlockNode::updateCleanSuccessors);

		for (ExceptionHandler eh : mth.getExceptionHandlers()) {
			removeMonitorExitFromExcHandler(mth, eh);
		}
		BlockProcessor.removeMarkedBlocks(mth);
		return true;
	}

	/**
	 * Wrap try blocks with top/bottom splitter and connect them to handler block.
	 * Sometimes try block can be handler block itself and should be connected before wrapping.
	 * Use queue for postpone try blocks not ready for wrap.
	 */
	private static void connectExcHandlers(MethodNode mth, List<TryCatchBlockAttr> tryBlocks) {
		if (tryBlocks.isEmpty()) {
			return;
		}
		int limit = tryBlocks.size() * 3;
		int count = 0;
		Deque<TryCatchBlockAttr> queue = new ArrayDeque<>(tryBlocks);
		while (!queue.isEmpty()) {
			TryCatchBlockAttr tryBlock = queue.removeFirst();
			boolean complete = wrapBlocksWithTryCatch(mth, tryBlock);
			if (!complete) {
				queue.addLast(tryBlock); // return to queue at the end
			}
			if (count++ > limit) {
				throw new JadxRuntimeException("Try blocks wrapping queue limit reached! Please report as an issue!");
			}
		}
	}

	private static void processCatchAttr(MethodNode mth) {
		for (BlockNode block : mth.getBasicBlocks()) {
			for (InsnNode insn : block.getInstructions()) {
				if (insn.contains(AType.EXC_CATCH) && !insn.canThrowException()) {
					insn.remove(AType.EXC_CATCH);
				}
			}
		}
		// if all instructions in block have same 'catch' attribute -> add this attribute for whole block.
		for (BlockNode block : mth.getBasicBlocks()) {
			CatchAttr commonCatchAttr = getCommonCatchAttr(block);
			if (commonCatchAttr != null) {
				block.addAttr(commonCatchAttr);
				for (InsnNode insn : block.getInstructions()) {
					if (insn.contains(AFlag.TRY_ENTER)) {
						block.add(AFlag.TRY_ENTER);
					}
					if (insn.contains(AFlag.TRY_LEAVE)) {
						block.add(AFlag.TRY_LEAVE);
					}
				}
			}
		}
	}

	@Nullable
	private static CatchAttr getCommonCatchAttr(BlockNode block) {
		CatchAttr commonCatchAttr = null;
		for (InsnNode insn : block.getInstructions()) {
			CatchAttr catchAttr = insn.get(AType.EXC_CATCH);
			if (catchAttr != null) {
				if (commonCatchAttr == null) {
					commonCatchAttr = catchAttr;
					continue;
				}
				if (!commonCatchAttr.equals(catchAttr)) {
					return null;
				}
			}
		}
		return commonCatchAttr;
	}

	@SuppressWarnings("ForLoopReplaceableByForEach")
	private static void initExcHandlers(MethodNode mth) {
		List<BlockNode> blocks = mth.getBasicBlocks();
		int blocksCount = blocks.size();
		for (int i = 0; i < blocksCount; i++) { // will add new blocks to list end
			BlockNode block = blocks.get(i);
			InsnNode firstInsn = BlockUtils.getFirstInsn(block);
			if (firstInsn == null) {
				continue;
			}
			ExcHandlerAttr excHandlerAttr = firstInsn.get(AType.EXC_HANDLER);
			if (excHandlerAttr == null) {
				continue;
			}
			firstInsn.remove(AType.EXC_HANDLER);
			removeTmpConnection(block);

			ExceptionHandler excHandler = excHandlerAttr.getHandler();
			if (block.getPredecessors().isEmpty()) {
				excHandler.setHandlerBlock(block);
				block.addAttr(excHandlerAttr);
				excHandler.addBlock(block);
				BlockUtils.collectBlocksDominatedByWithExcHandlers(mth, block, block)
						.forEach(excHandler::addBlock);
			} else {
				// ignore already connected handlers -> make catch empty
				BlockNode emptyHandlerBlock = BlockSplitter.startNewBlock(mth, block.getStartOffset());
				emptyHandlerBlock.add(AFlag.SYNTHETIC);
				emptyHandlerBlock.addAttr(excHandlerAttr);
				BlockSplitter.connect(emptyHandlerBlock, block);
				excHandler.setHandlerBlock(emptyHandlerBlock);
				excHandler.addBlock(emptyHandlerBlock);
			}
			fixMoveExceptionInsn(block, excHandlerAttr);
		}
	}

	private static void removeTmpConnection(BlockNode block) {
		TmpEdgeAttr tmpEdgeAttr = block.get(AType.TMP_EDGE);
		if (tmpEdgeAttr != null) {
			// remove temp connection
			BlockSplitter.removeConnection(tmpEdgeAttr.getBlock(), block);
			block.remove(AType.TMP_EDGE);
		}
	}

	private static List<TryCatchBlockAttr> prepareTryBlocks(MethodNode mth) {
		Map<ExceptionHandler, List<BlockNode>> blocksByHandler = new HashMap<>();
		for (BlockNode block : mth.getBasicBlocks()) {
			CatchAttr catchAttr = block.get(AType.EXC_CATCH);
			if (catchAttr != null) {
				for (ExceptionHandler eh : catchAttr.getHandlers()) {
					blocksByHandler
							.computeIfAbsent(eh, c -> new ArrayList<>())
							.add(block);
				}
			}
		}
		if (Consts.DEBUG_EXC_HANDLERS) {
			LOG.debug("Input exception handlers:");
			blocksByHandler.forEach((eh, blocks) -> LOG.debug(" {}, throw blocks: {}, handler blocks: {}", eh, blocks, eh.getBlocks()));
		}
		if (blocksByHandler.isEmpty()) {
			// no catch blocks -> remove all handlers
			mth.getExceptionHandlers().forEach(eh -> removeExcHandler(mth, eh));
		} else {
			// remove handlers without blocks in catch attribute
			blocksByHandler.forEach((eh, blocks) -> {
				if (blocks.isEmpty()) {
					removeExcHandler(mth, eh);
				}
			});
		}
		BlockSplitter.detachMarkedBlocks(mth);
		mth.clearExceptionHandlers();
		if (mth.isNoExceptionHandlers()) {
			return Collections.emptyList();
		}

		blocksByHandler.forEach((eh, blocks) -> {
			// remove catches from same handler
			blocks.removeAll(eh.getBlocks());
		});

		List<TryCatchBlockAttr> tryBlocks = new ArrayList<>();
		blocksByHandler.forEach((eh, blocks) -> {
			List<ExceptionHandler> handlers = new ArrayList<>(1);
			handlers.add(eh);
			tryBlocks.add(new TryCatchBlockAttr(tryBlocks.size(), handlers, blocks));
		});
		if (tryBlocks.size() > 1) {
			// merge or mark as outer/inner
			while (true) {
				boolean restart = combineTryCatchBlocks(tryBlocks);
				if (!restart) {
					break;
				}
			}
		}
		checkForMultiCatch(mth, tryBlocks);
		clearTryBlocks(mth, tryBlocks);
		sortHandlers(mth, tryBlocks);

		if (Consts.DEBUG_EXC_HANDLERS) {
			LOG.debug("Result try-catch blocks:");
			tryBlocks.forEach(tryBlock -> LOG.debug(" {}", tryBlock));
		}
		return tryBlocks;
	}

	private static void clearTryBlocks(MethodNode mth, List<TryCatchBlockAttr> tryBlocks) {
		tryBlocks.forEach(tc -> tc.getBlocks().removeIf(b -> b.contains(AFlag.REMOVE)));
		tryBlocks.removeIf(tb -> tb.getBlocks().isEmpty() || tb.getHandlers().isEmpty());
		mth.clearExceptionHandlers();
		BlockSplitter.detachMarkedBlocks(mth);
	}

	private static boolean combineTryCatchBlocks(List<TryCatchBlockAttr> tryBlocks) {
		for (TryCatchBlockAttr outerTryBlock : tryBlocks) {
			for (TryCatchBlockAttr innerTryBlock : tryBlocks) {
				if (outerTryBlock == innerTryBlock || innerTryBlock.getOuterTryBlock() != null) {
					continue;
				}
				if (checkTryCatchRelation(tryBlocks, outerTryBlock, innerTryBlock)) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean checkTryCatchRelation(List<TryCatchBlockAttr> tryBlocks,
			TryCatchBlockAttr outerTryBlock, TryCatchBlockAttr innerTryBlock) {
		if (outerTryBlock.getBlocks().equals(innerTryBlock.getBlocks())) {
			// same try blocks -> merge handlers
			List<ExceptionHandler> handlers = Utils.concatDistinct(outerTryBlock.getHandlers(), innerTryBlock.getHandlers());
			tryBlocks.add(new TryCatchBlockAttr(tryBlocks.size(), handlers, outerTryBlock.getBlocks()));
			tryBlocks.remove(outerTryBlock);
			tryBlocks.remove(innerTryBlock);
			return true;
		}

		Set<BlockNode> handlerBlocks = innerTryBlock.getHandlers().stream()
				.flatMap(eh -> eh.getBlocks().stream())
				.collect(Collectors.toSet());
		boolean catchInHandler = handlerBlocks.stream().anyMatch(isHandlersIntersects(outerTryBlock));
		boolean catchInTry = innerTryBlock.getBlocks().stream().anyMatch(isHandlersIntersects(outerTryBlock));
		boolean blocksOutsideHandler = outerTryBlock.getBlocks().stream().anyMatch(b -> !handlerBlocks.contains(b));

		boolean makeInner = catchInHandler && (catchInTry || blocksOutsideHandler);
		if (makeInner && innerTryBlock.isAllHandler()) {
			// inner try block can't have catch-all handler
			outerTryBlock.setBlocks(Utils.concatDistinct(outerTryBlock.getBlocks(), innerTryBlock.getBlocks()));
			innerTryBlock.clear();
			return false;
		}
		if (makeInner) {
			// convert to inner
			List<BlockNode> mergedBlocks = Utils.concatDistinct(outerTryBlock.getBlocks(), innerTryBlock.getBlocks());
			innerTryBlock.getHandlers().removeAll(outerTryBlock.getHandlers());
			innerTryBlock.setOuterTryBlock(outerTryBlock);
			outerTryBlock.addInnerTryBlock(innerTryBlock);
			outerTryBlock.setBlocks(mergedBlocks);
			return false;
		}
		if (innerTryBlock.getHandlers().containsAll(outerTryBlock.getHandlers())) {
			// merge
			List<BlockNode> mergedBlocks = Utils.concatDistinct(outerTryBlock.getBlocks(), innerTryBlock.getBlocks());
			List<ExceptionHandler> handlers = Utils.concatDistinct(outerTryBlock.getHandlers(), innerTryBlock.getHandlers());
			tryBlocks.add(new TryCatchBlockAttr(tryBlocks.size(), handlers, mergedBlocks));
			tryBlocks.remove(outerTryBlock);
			tryBlocks.remove(innerTryBlock);
			return true;
		}
		return false;
	}

	@NotNull
	private static Predicate<BlockNode> isHandlersIntersects(TryCatchBlockAttr outerTryBlock) {
		return block -> {
			CatchAttr catchAttr = block.get(AType.EXC_CATCH);
			return catchAttr != null && Objects.equals(catchAttr.getHandlers(), outerTryBlock.getHandlers());
		};
	}

	private static void removeExcHandler(MethodNode mth, ExceptionHandler excHandler) {
		excHandler.markForRemove();
		BlockSplitter.removeConnection(mth.getEnterBlock(), excHandler.getHandlerBlock());
	}

	private static boolean wrapBlocksWithTryCatch(MethodNode mth, TryCatchBlockAttr tryCatchBlock) {
		List<BlockNode> blocks = tryCatchBlock.getBlocks();
		BlockNode top = searchTopBlock(mth, blocks);
		if (top.getPredecessors().isEmpty() && top != mth.getEnterBlock()) {
			return false;
		}
		BlockNode bottom = searchBottomBlock(mth, blocks);
		if (Consts.DEBUG_EXC_HANDLERS) {
			LOG.debug("TryCatch #{} split: top {}, bottom: {}", tryCatchBlock.id(), top, bottom);
		}
		BlockNode topSplitterBlock = getTopSplitterBlock(mth, top);
		topSplitterBlock.add(AFlag.EXC_TOP_SPLITTER);
		topSplitterBlock.add(AFlag.SYNTHETIC);

		int totalHandlerBlocks = tryCatchBlock.getHandlers().stream().mapToInt(eh -> eh.getBlocks().size()).sum();

		BlockNode bottomSplitterBlock;
		if (bottom == null || totalHandlerBlocks == 0) {
			bottomSplitterBlock = null;
		} else {
			BlockNode existBottomSplitter = BlockUtils.getBlockWithFlag(bottom.getSuccessors(), AFlag.EXC_BOTTOM_SPLITTER);
			bottomSplitterBlock = existBottomSplitter != null ? existBottomSplitter : BlockSplitter.startNewBlock(mth, -1);
			bottomSplitterBlock.add(AFlag.EXC_BOTTOM_SPLITTER);
			bottomSplitterBlock.add(AFlag.SYNTHETIC);
			BlockSplitter.connect(bottom, bottomSplitterBlock);
		}

		if (Consts.DEBUG_EXC_HANDLERS) {
			LOG.debug("TryCatch #{} result splitters: top {}, bottom: {}",
					tryCatchBlock.id(), topSplitterBlock, bottomSplitterBlock);
		}
		connectSplittersAndHandlers(tryCatchBlock, topSplitterBlock, bottomSplitterBlock);

		for (BlockNode block : blocks) {
			TryCatchBlockAttr currentTCBAttr = block.get(AType.TRY_BLOCK);
			if (currentTCBAttr == null || currentTCBAttr.getInnerTryBlocks().contains(tryCatchBlock)) {
				block.addAttr(tryCatchBlock);
			}
		}
		tryCatchBlock.setTopSplitter(topSplitterBlock);

		topSplitterBlock.updateCleanSuccessors();
		if (bottomSplitterBlock != null) {
			bottomSplitterBlock.updateCleanSuccessors();
		}
		return true;
	}

	private static BlockNode getTopSplitterBlock(MethodNode mth, BlockNode top) {
		if (top == mth.getEnterBlock()) {
			BlockNode fixedTop = mth.getEnterBlock().getSuccessors().get(0);
			return BlockSplitter.blockSplitTop(mth, fixedTop);
		}
		BlockNode existPredTopSplitter = BlockUtils.getBlockWithFlag(top.getPredecessors(), AFlag.EXC_TOP_SPLITTER);
		if (existPredTopSplitter != null) {
			return existPredTopSplitter;
		}
		// try to reuse exists splitter on empty simple path below top block
		if (top.getCleanSuccessors().size() == 1 && top.getInstructions().isEmpty()) {
			BlockNode otherTopSplitter = BlockUtils.getBlockWithFlag(top.getCleanSuccessors(), AFlag.EXC_TOP_SPLITTER);
			if (otherTopSplitter != null && otherTopSplitter.getPredecessors().size() == 1) {
				return otherTopSplitter;
			}
		}
		return BlockSplitter.blockSplitTop(mth, top);
	}

	private static BlockNode searchTopBlock(MethodNode mth, List<BlockNode> blocks) {
		BlockNode top = BlockUtils.getTopBlock(blocks);
		if (top != null) {
			return adjustTopBlock(top);
		}
		BlockNode topDom = BlockUtils.getCommonDominator(mth, blocks);
		if (topDom != null) {
			// dominator always return one up block if blocks already contains dominator, use successor instead
			if (topDom.getSuccessors().size() == 1) {
				BlockNode upBlock = topDom.getSuccessors().get(0);
				if (blocks.contains(upBlock)) {
					return upBlock;
				}
			}
			return adjustTopBlock(topDom);
		}
		throw new JadxRuntimeException("Failed to find top block for try-catch from: " + blocks);
	}

	private static BlockNode adjustTopBlock(BlockNode topBlock) {
		if (topBlock.getSuccessors().size() == 1 && !topBlock.contains(AType.EXC_CATCH)) {
			// top block can be lifted by other exception handlers included in blocks list, trying to undo that
			return topBlock.getSuccessors().get(0);
		}
		return topBlock;
	}

	@Nullable
	private static BlockNode searchBottomBlock(MethodNode mth, List<BlockNode> blocks) {
		// search common post-dominator block inside input set
		BlockNode bottom = BlockUtils.getBottomBlock(blocks);
		if (bottom != null) {
			return bottom;
		}
		// not found -> blocks don't have same dominator
		// try to search common cross block outside input set
		// NOTE: bottom block not needed for exit nodes (no data flow from them)
		BlockNode pathCross = BlockUtils.getPathCross(mth, blocks);
		if (pathCross == null) {
			return null;
		}
		List<BlockNode> preds = new ArrayList<>(pathCross.getPredecessors());
		preds.removeAll(blocks);
		List<BlockNode> outsidePredecessors = preds.stream()
				.filter(p -> !BlockUtils.atLeastOnePathExists(blocks, p))
				.collect(Collectors.toList());
		if (outsidePredecessors.isEmpty()) {
			return pathCross;
		}
		// some predecessors outside of input set paths -> split block only for input set
		BlockNode splitCross = BlockSplitter.blockSplitTop(mth, pathCross);
		splitCross.add(AFlag.SYNTHETIC);
		for (BlockNode outsidePredecessor : outsidePredecessors) {
			// return predecessors to split bottom block (original)
			BlockSplitter.replaceConnection(outsidePredecessor, splitCross, pathCross);
		}
		return splitCross;
	}

	private static void connectSplittersAndHandlers(TryCatchBlockAttr tryCatchBlock, BlockNode topSplitterBlock,
			@Nullable BlockNode bottomSplitterBlock) {
		for (ExceptionHandler handler : tryCatchBlock.getHandlers()) {
			BlockNode handlerBlock = handler.getHandlerBlock();
			BlockSplitter.connect(topSplitterBlock, handlerBlock);
			if (bottomSplitterBlock != null) {
				BlockSplitter.connect(bottomSplitterBlock, handlerBlock);
			}
		}
		TryCatchBlockAttr outerTryBlock = tryCatchBlock.getOuterTryBlock();
		if (outerTryBlock != null) {
			connectSplittersAndHandlers(outerTryBlock, topSplitterBlock, bottomSplitterBlock);
		}
	}

	private static void fixMoveExceptionInsn(BlockNode block, ExcHandlerAttr excHandlerAttr) {
		ExceptionHandler excHandler = excHandlerAttr.getHandler();
		ArgType argType = excHandler.getArgType();
		InsnNode me = BlockUtils.getLastInsn(block);
		if (me != null && me.getType() == InsnType.MOVE_EXCEPTION) {
			// set correct type for 'move-exception' operation
			RegisterArg resArg = InsnArg.reg(me.getResult().getRegNum(), argType);
			resArg.copyAttributesFrom(me);
			me.setResult(resArg);
			me.add(AFlag.DONT_INLINE);
			resArg.add(AFlag.CUSTOM_DECLARE);
			excHandler.setArg(resArg);
			me.addAttr(excHandlerAttr);
			return;
		}
		// handler arguments not used
		excHandler.setArg(new NamedArg("unused", argType));
	}

	private static void removeMonitorExitFromExcHandler(MethodNode mth, ExceptionHandler excHandler) {
		for (BlockNode excBlock : excHandler.getBlocks()) {
			InsnRemover remover = new InsnRemover(mth, excBlock);
			for (InsnNode insn : excBlock.getInstructions()) {
				if (insn.getType() == InsnType.MONITOR_ENTER) {
					break;
				}
				if (insn.getType() == InsnType.MONITOR_EXIT) {
					remover.addAndUnbind(insn);
				}
			}
			remover.perform();
		}
	}

	private static void checkForMultiCatch(MethodNode mth, List<TryCatchBlockAttr> tryBlocks) {
		boolean merged = false;
		for (TryCatchBlockAttr tryBlock : tryBlocks) {
			if (mergeMultiCatch(mth, tryBlock)) {
				merged = true;
			}
		}
		if (merged) {
			BlockSplitter.detachMarkedBlocks(mth);
			mth.clearExceptionHandlers();
		}
	}

	private static boolean mergeMultiCatch(MethodNode mth, TryCatchBlockAttr tryCatch) {
		if (tryCatch.getHandlers().size() < 2) {
			return false;
		}
		for (ExceptionHandler handler : tryCatch.getHandlers()) {
			if (handler.getBlocks().size() != 1) {
				return false;
			}
			BlockNode block = handler.getHandlerBlock();
			if (block.getInstructions().size() != 1
					|| !BlockUtils.checkLastInsnType(block, InsnType.MOVE_EXCEPTION)) {
				return false;
			}
		}
		List<BlockNode> handlerBlocks = ListUtils.map(tryCatch.getHandlers(), ExceptionHandler::getHandlerBlock);
		List<BlockNode> successorBlocks = handlerBlocks.stream()
				.flatMap(h -> h.getSuccessors().stream())
				.distinct()
				.collect(Collectors.toList());
		if (successorBlocks.size() != 1) {
			return false;
		}
		BlockNode successorBlock = successorBlocks.get(0);
		if (!ListUtils.unorderedEquals(successorBlock.getPredecessors(), handlerBlocks)) {
			return false;
		}
		List<RegisterArg> regs = tryCatch.getHandlers().stream()
				.map(h -> Objects.requireNonNull(BlockUtils.getLastInsn(h.getHandlerBlock())).getResult())
				.distinct()
				.collect(Collectors.toList());
		if (regs.size() != 1) {
			return false;
		}

		// merge confirm, leave only first handler, remove others
		ExceptionHandler resultHandler = tryCatch.getHandlers().get(0);
		tryCatch.getHandlers().removeIf(handler -> {
			if (handler == resultHandler) {
				return false;
			}
			resultHandler.addCatchTypes(mth, handler.getCatchTypes());
			handler.markForRemove();
			return true;
		});
		return true;
	}

	private static void sortHandlers(MethodNode mth, List<TryCatchBlockAttr> tryBlocks) {
		TypeCompare typeCompare = mth.root().getTypeCompare();
		Comparator<ArgType> comparator = typeCompare.getReversedComparator();
		for (TryCatchBlockAttr tryBlock : tryBlocks) {
			for (ExceptionHandler handler : tryBlock.getHandlers()) {
				handler.getCatchTypes().sort((first, second) -> compareByTypeAndName(comparator, first, second));
			}
			tryBlock.getHandlers().sort((first, second) -> {
				if (first.equals(second)) {
					throw new JadxRuntimeException("Same handlers in try block: " + tryBlock);
				}
				if (first.isCatchAll()) {
					return 1;
				}
				if (second.isCatchAll()) {
					return -1;
				}
				return compareByTypeAndName(comparator,
						ListUtils.first(first.getCatchTypes()), ListUtils.first(second.getCatchTypes()));
			});
		}
	}

	@SuppressWarnings("ComparatorResultComparison")
	private static int compareByTypeAndName(Comparator<ArgType> comparator, ClassInfo first, ClassInfo second) {
		int r = comparator.compare(first.getType(), second.getType());
		if (r == -2) {
			// on conflict sort by name
			return first.compareTo(second);
		}
		return r;
	}
}
