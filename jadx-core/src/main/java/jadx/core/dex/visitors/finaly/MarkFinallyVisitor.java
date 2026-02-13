package jadx.core.dex.visitors.finaly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.instructions.InsnType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.instructions.args.SSAVar;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.trycatch.ExceptionHandler;
import jadx.core.dex.trycatch.TryCatchBlockAttr;
import jadx.core.dex.trycatch.TryEdge;
import jadx.core.dex.trycatch.TryEdgeScopeGroupMap;
import jadx.core.dex.visitors.AbstractVisitor;
import jadx.core.dex.visitors.ConstInlineVisitor;
import jadx.core.dex.visitors.DepthTraversal;
import jadx.core.dex.visitors.IDexTreeVisitor;
import jadx.core.dex.visitors.JadxVisitor;
import jadx.core.dex.visitors.finaly.traverser.TraverserController;
import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.ssa.SSATransform;
import jadx.core.utils.BlockUtils;
import jadx.core.utils.ListUtils;
import jadx.core.utils.Pair;
import jadx.core.utils.exceptions.DecodeException;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * This visitor is responsible for extracting finally blocks from duplicated instructions located
 * within the ends of each branch leading to the terminating point of all code paths.
 *
 * To do this, the terminating point of each handler / exit from try body is found relative to every
 * other handler / exit from try body. This, in effect, is used to identify the "scopes" of each
 * possible path within the try block and thus can be used to find a common series of included
 * blocks
 * within the "scope" of each handler and a block to start searching from in reverse to find common
 * instructions between that and the "nominated finally" handler. These groups are described by the
 * {@link TryEdgeScopeGroupMap} object.
 *
 * After this, the {@link TraverserController} is responsible for traversing the block graphs from
 * each "scope terminus" along the blocks contained with each handlers "scope", comparing them
 * against
 * the "nominated finally" block. If the control flow and instructions of each block match, then
 * they
 * are added as duplicate instructions. At the end, the visitor will mark the identified duplicated
 * instructions and identified finally instructions with the respective {@link AFlag} to be handled
 * during regioning of the block graph.
 */
@JadxVisitor(
		name = "MarkFinallyVisitor",
		desc = "Search and mark duplicate code generated for finally block",
		runAfter = SSATransform.class,
		runBefore = ConstInlineVisitor.class
)
public class MarkFinallyVisitor extends AbstractVisitor {

	private static final Logger LOG = LoggerFactory.getLogger(MarkFinallyVisitor.class);

	private static final class TryExtractInfo {

		private final TryCatchBlockAttr tryBlock;
		private final TryEdgeScopeGroupMap scopeGroups;
		private final ExceptionHandler finallyHandler;
		private final Map<BlockNode, List<TryEdge>> scopeTerminusGroups;
		private final TryCatchEdgeBlockMap handlerScopes;
		private final Set<BlockNode> allHandlerBlocks;
		private final Set<BlockNode> rethrowBlocks;

		private Set<BlockNode> completeFinallyBlocks = null;
		private Set<BlockNode> completeCandidateBlocks = null;

		private TryExtractInfo(final TryCatchBlockAttr tryBlock, final TryEdgeScopeGroupMap scopeGroups,
				final ExceptionHandler finallyHandler, final Map<BlockNode, List<TryEdge>> fallthroughGroups,
				final TryCatchEdgeBlockMap handlerScopes) {
			this.tryBlock = tryBlock;
			this.scopeGroups = scopeGroups;
			this.finallyHandler = finallyHandler;
			this.scopeTerminusGroups = fallthroughGroups;
			this.handlerScopes = handlerScopes;
			this.allHandlerBlocks = new HashSet<>();
			this.rethrowBlocks = new HashSet<>();

			for (final List<BlockNode> handlerBlocks : handlerScopes.values()) {
				allHandlerBlocks.addAll(handlerBlocks);
			}
		}
	}

	@Override
	public void visit(final MethodNode mth) {
		if (mth.isNoCode() || mth.isNoExceptionHandlers()) {
			return;
		}
		try {
			boolean implicitHandlerRemoved = false;
			final List<TryCatchBlockAttr> tryBlocks = mth.getAll(AType.TRY_BLOCKS_LIST);

			final List<TryCatchBlockAttr> processRequiredTryBlocks = new ArrayList<>();

			// Search through all exception handlers and:
			// - Remove implicit handlers
			// - Mark non-implicit handlers to be searched for a finally block
			for (final TryCatchBlockAttr tryBlock : tryBlocks) {
				final TryExtractInfo tryInfo = getTryBlockData(mth, tryBlock);
				if (tryInfo == null) {
					continue;
				}
				final List<BlockNode> cutHandlerBlocks = cutHandlerBlocks(tryInfo, tryInfo.finallyHandler);
				if (cutHandlerBlocks == null) {
					continue;
				}
				if (attemptRemoveImplicitHandlers(cutHandlerBlocks, tryInfo)) {
					implicitHandlerRemoved = true;
				} else {
					processRequiredTryBlocks.add(tryBlock);
				}
			}
			// If any implicit handlers have been found, remove them
			if (implicitHandlerRemoved) {
				resetTryBlocks(mth, tryBlocks);
			}

			// Search through all non-implicit handlers and search for a finally block.
			boolean finallyExtracted = false;
			for (final TryCatchBlockAttr tryBlock : processRequiredTryBlocks) {
				// Refresh scope groups now due to implicit handlers
				final TryExtractInfo tryInfo = getTryBlockData(mth, tryBlock);

				if (tryInfo == null) {
					continue;
				}

				cutHandlerBlocks(tryInfo, tryInfo.finallyHandler);

				finallyExtracted |= processTryBlock(mth, tryInfo);
			}
			// If any handlers have been merged, remove them
			if (finallyExtracted) {
				resetTryBlocks(mth, tryBlocks);
			}
		} catch (final Exception e) {
			LOG.error(e.getMessage());
			undoFinallyVisitor(mth);
			mth.addWarnComment("Undo finally extract visitor", e);
		}
	}

	private static void resetTryBlocks(final MethodNode mth, final List<TryCatchBlockAttr> tryBlocks) {
		mth.clearExceptionHandlers();
		// remove merged or empty try blocks from list in method attribute
		final List<TryCatchBlockAttr> clearedTryBlocks = new ArrayList<>(tryBlocks);
		if (clearedTryBlocks.removeIf(TryCatchBlockAttr::isImplicitOrMerged)) {
			mth.remove(AType.TRY_BLOCKS_LIST);
			mth.addAttr(AType.TRY_BLOCKS_LIST, clearedTryBlocks);
		}
	}

	/**
	 * For a given try block, attempts to calculate try block data. This includes the handler blocks for
	 * each try branch, data regarding the scope of each try branch relative to every other branch, and
	 * the blocks logically contained within each try branch. This information is stored via internal
	 * class members and is not returned by the function.
	 *
	 * @param mth      The method containing the try block.
	 * @param tryBlock The try block to determine the scope information of.
	 * @return The handler identified as the "all" handler.
	 */
	@Nullable
	private static TryExtractInfo getTryBlockData(final MethodNode mth, final TryCatchBlockAttr tryBlock) {
		if (tryBlock.isMerged()) {
			return null;
		}

		// Find the all handler
		ExceptionHandler allHandler = null;
		for (final ExceptionHandler excHandler : tryBlock.getHandlers()) {
			if (excHandler.isCatchAll()) {
				allHandler = excHandler;
				break;
			}
		}

		if (allHandler == null) {
			return null;
		}

		final TryEdgeScopeGroupMap scopeGroups = tryBlock.getExecutionScopeGroups(mth);
		final var fallthroughGroups = tryBlock.getHandlerFallthroughGroups(mth, scopeGroups);
		final var handlerScopes = TryCatchEdgeBlockMap.getAllInScope(mth, tryBlock, scopeGroups, allHandler, fallthroughGroups);
		return new TryExtractInfo(tryBlock, scopeGroups, allHandler, fallthroughGroups, handlerScopes);
	}

	/**
	 * Processes a try block, attempting to extract a finally by locating common instruction patterns
	 * between all
	 * try branches.
	 *
	 * @param mth     The method containing the try block.
	 * @param tryInfo The try block information.
	 * @return Whether a finally block has been successfully extracted.
	 */
	private static boolean processTryBlock(final MethodNode mth, final TryExtractInfo tryInfo) {
		if (tryInfo.rethrowBlocks.isEmpty()) {
			return false;
		}

		if (extractFinally(mth, tryInfo)) {
			for (final BlockNode rethrowBlock : tryInfo.rethrowBlocks) {
				final InsnNode lastInsn = BlockUtils.getLastInsn(rethrowBlock);
				if (lastInsn == null) {
					continue;
				}
				lastInsn.add(AFlag.DONT_GENERATE);
			}
			return true;
		}
		return false;
	}

	@Nullable
	private static List<BlockNode> cutHandlerBlocks(final TryExtractInfo tryInfo, final ExceptionHandler handler) {
		final BlockNode handlerBlock = handler.getHandlerBlock();
		final List<BlockNode> handlerBlocks = tryInfo.handlerScopes.getBlocksForHandler(handler);
		if (handlerBlocks == null) {
			return null;
		}

		final InsnNode handlerFinalInsn = BlockUtils.getFirstInsn(handlerBlock);
		if (handlerFinalInsn != null && handlerFinalInsn.getType() == InsnType.MOVE_EXCEPTION) {
			handlerBlocks.remove(handlerBlock); // exclude block with 'move-exception'
		}

		final BlockNode bottomBlock = BlockUtils.getBottomBlock(handlerBlocks);
		final List<BlockNode> pathExits = BlockUtils.followEmptyUpPathWithinSet(bottomBlock, handlerBlocks);
		if (pathExits.isEmpty()) {
			return handlerBlocks;
		}
		for (final BlockNode pathExit : pathExits) {
			// For this to be able to extract a finally, we must ensure that all paths into the handler's logic
			// end with a THROW equal to the output of the move-exception instruction located at the start of
			// this handler, if any.
			final InsnNode bottomBlockLastInsn = BlockUtils.getLastInsn(pathExit);
			final boolean isValidPathExit = bottomBlockLastInsn != null
					&& handlerFinalInsn != null
					&& bottomBlockLastInsn.getType() == InsnType.THROW
					&& bottomBlockLastInsn.getArgsCount() > 0
					&& bottomBlockLastInsn.getArg(0).equals(handlerFinalInsn.getResult());
			if (!isValidPathExit) {
				return handlerBlocks;
			}
		}
		final List<BlockNode> cutHandlerBlocks = new ArrayList<>(handlerBlocks);
		for (final BlockNode pathExit : pathExits) {
			cutHandlerBlocks.remove(pathExit);
			removeEmptyUpPath(cutHandlerBlocks, pathExit);
			tryInfo.rethrowBlocks.add(pathExit);
		}
		return cutHandlerBlocks;
	}

	/**
	 * Attempts to identify and remove an implicit try catch block.
	 *
	 * @param cutHandlerBlocks The cut handler blocks of the all handler.
	 * @return Whether the try block is implicit and has been removed.
	 */
	private static boolean attemptRemoveImplicitHandlers(final List<BlockNode> cutHandlerBlocks, final TryExtractInfo tryInfo) {
		if (!(cutHandlerBlocks.isEmpty() || BlockUtils.isAllBlocksEmpty(cutHandlerBlocks))) {
			return false;
		}
		// remove empty catch
		tryInfo.finallyHandler.getTryBlock().removeHandler(tryInfo.finallyHandler);
		return true;
	}

	/**
	 * Search and mark common code from 'try' block and 'handlers'.
	 */
	private static boolean extractFinally(final MethodNode mth, final TryExtractInfo tryInfo) {
		// Get all handlers from this and inner try blocks.
		final boolean hasInnerBlocks = !tryInfo.tryBlock.getInnerTryBlocks().isEmpty();
		final List<ExceptionHandler> handlers = getHandlersForTryCatch(tryInfo.tryBlock);
		if (handlers.isEmpty()) {
			return false;
		}

		final Map<InsnNode, List<InsnNode>> insns = findCommonInsns(mth, tryInfo);
		if (insns == null || insns.isEmpty()) {
			return false;
		}

		final Set<InsnNode> ignoredFinallyInsns = new HashSet<>();
		final Set<InsnNode> ignoredCandidateInsns = new HashSet<>();
		final Map<InsnNode, List<InsnNode>> insnMap = new HashMap<>();
		for (final InsnNode finallyInsn : insns.keySet()) {
			final List<InsnNode> candidateInsns = insns.get(finallyInsn);

			// For an instruction to have matched, the number of times it has been found must be
			// equal to the number of edges that the exception handler has which aren't the
			// finally handler.
			if (candidateInsns.size() != tryInfo.handlerScopes.size() - 1) {
				ignoredFinallyInsns.add(finallyInsn);
				ignoredCandidateInsns.addAll(candidateInsns);
				// TODO: Add support for partial `catch (Throwable)` finally clauses.
				// continue;
				return false;
			}

			insnMap.put(finallyInsn, candidateInsns);
		}

		for (final InsnNode finallyInsn : insnMap.keySet()) {
			finallyInsn.add(AFlag.FINALLY_INSNS);
			final List<InsnNode> candidateInsns = insnMap.get(finallyInsn);
			for (final InsnNode candidateInsn : candidateInsns) {
				copyCodeVars(finallyInsn, candidateInsn);
				candidateInsn.add(AFlag.DONT_GENERATE);
			}
		}

		for (final BlockNode finallyBlock : tryInfo.completeFinallyBlocks) {
			if (ListUtils.anyMatch(finallyBlock.getInstructions(), ignoredFinallyInsns::contains)) {
				// If this block contains an instruction which was not found in all try edges,
				// don't mark it as a finally block.
				continue;
			}
			finallyBlock.add(AFlag.FINALLY_INSNS);
		}
		for (final BlockNode candidateBlock : tryInfo.completeCandidateBlocks) {
			if (ListUtils.anyMatch(candidateBlock.getInstructions(), ignoredCandidateInsns::contains)) {
				// If this block contains an instruction which was found to "duplicate" a finally
				// instruction which was not found in all try edges, don't mark it as a duplicated
				// block.
				continue;
			}
			candidateBlock.add(AFlag.DONT_GENERATE);
		}

		// If any scope has been merged with the fallthrough case of the try catch, don't merge inner trys.
		// Otherwise, merge inner trys.
		final boolean mergedFallthroughScope =
				ListUtils.anyMatch(tryInfo.scopeGroups.getMergedScopes(), scopePair -> scopePair.getFirst().isNotHandlerExit());
		final boolean mergeInnerTryBlocks = hasInnerBlocks && !mergedFallthroughScope;

		tryInfo.finallyHandler.setFinally(true);

		if (mergeInnerTryBlocks) {
			final List<TryCatchBlockAttr> innerTryBlocks = tryInfo.tryBlock.getInnerTryBlocks();
			for (final TryCatchBlockAttr innerTryBlock : innerTryBlocks) {
				tryInfo.tryBlock.getHandlers().addAll(innerTryBlock.getHandlers());
				tryInfo.tryBlock.getBlocks().addAll(innerTryBlock.getBlocks());
				innerTryBlock.setMerged(true);
			}
			tryInfo.tryBlock.setBlocks(ListUtils.distinctList(tryInfo.tryBlock.getBlocks()));
			innerTryBlocks.clear();
		}

		return true;
	}

	/**
	 * Gets a list of every exception handler attached to this try block, including handlers of inner
	 * try blocks.
	 *
	 * @param tryBlock The source try block to get the list of exception handlers for
	 * @return The list of exception handlers.
	 */
	private static List<ExceptionHandler> getHandlersForTryCatch(final TryCatchBlockAttr tryBlock) {
		final boolean hasInnerBlocks = !tryBlock.getInnerTryBlocks().isEmpty();
		final List<ExceptionHandler> handlers;
		if (hasInnerBlocks) {
			// collect handlers from this and all inner blocks
			handlers = new ArrayList<>(tryBlock.getHandlers());
			for (final TryCatchBlockAttr innerTryBlock : tryBlock.getInnerTryBlocks()) {
				handlers.addAll(getHandlersForTryCatch(innerTryBlock));
			}
		} else {
			handlers = tryBlock.getHandlers();
		}
		return handlers;
	}

	@Nullable
	private static Map<InsnNode, List<InsnNode>> findCommonInsns(final MethodNode mth, final TryExtractInfo tryInfo) {
		final List<BlockNode> allHandlerBlocks = tryInfo.handlerScopes.getBlocksForHandler(tryInfo.finallyHandler);
		final BlockNode finallyScopeTerminus = getTerminusForHandler(tryInfo.finallyHandler, tryInfo);

		final Map<InsnNode, List<InsnNode>> matchingInsns = new HashMap<>();
		for (final TryEdge edge : tryInfo.handlerScopes.keySet()) {
			if (edge.isHandlerExit() && edge.getExceptionHandler() == tryInfo.finallyHandler) {
				continue;
			}

			final List<BlockNode> handlerBlocks = tryInfo.handlerScopes.get(edge);
			BlockNode scopeTerminus = null;
			for (final BlockNode edgeTerminusBlock : tryInfo.scopeTerminusGroups.keySet()) {
				final List<TryEdge> edgesWithTerminus = tryInfo.scopeTerminusGroups.get(edgeTerminusBlock);
				if (edgesWithTerminus.contains(edge)) {
					scopeTerminus = edgeTerminusBlock;
					break;
				}
			}
			if (scopeTerminus == null) {
				throw new JadxRuntimeException("Expected to find fallthrough terminus for handler " + edge);
			}

			final TraverserActivePathState comparatorState =
					new TraverserActivePathState(mth, new SameInstructionsStrategyImpl(), finallyScopeTerminus,
							scopeTerminus, allHandlerBlocks, handlerBlocks);
			final TraverserController controller = new TraverserController();
			final List<TraverserActivePathState> pathResults;
			try {
				pathResults = controller.process(comparatorState);
			} catch (final TraverserException e) {
				LOG.error("Could not search for finally duplicate instructions in path", e);
				return null;
			}

			final Set<BlockNode> completeFinally = new HashSet<>();
			final Set<BlockNode> completeCandidate = new HashSet<>();
			for (final TraverserActivePathState pathResult : pathResults) {
				for (final Pair<InsnNode> matchingInsnPair : pathResult.getMatchedInsns()) {
					final InsnNode finallyInsn = matchingInsnPair.getFirst();
					final InsnNode candidateInsn = matchingInsnPair.getSecond();
					final List<InsnNode> candidateInsnsList;
					if (!matchingInsns.containsKey(finallyInsn)) {
						candidateInsnsList = new LinkedList<>();
						matchingInsns.put(finallyInsn, candidateInsnsList);
					} else {
						candidateInsnsList = matchingInsns.get(finallyInsn);
					}
					candidateInsnsList.add(candidateInsn);
				}

				completeFinally.addAll(pathResult.getAllFullyMatchedFinallyBlocks());
				completeCandidate.addAll(pathResult.getAllFullyMatchedCandidateBlocks());
			}

			if (tryInfo.completeFinallyBlocks == null) {
				tryInfo.completeFinallyBlocks = completeFinally;
			} else {
				tryInfo.completeFinallyBlocks.retainAll(completeFinally);
			}

			if (tryInfo.completeCandidateBlocks == null) {
				tryInfo.completeCandidateBlocks = completeCandidate;
			} else {
				tryInfo.completeCandidateBlocks.addAll(completeCandidate);
			}
		}

		return matchingInsns;
	}

	private static void removeEmptyUpPath(final List<BlockNode> handlerBlocks, final BlockNode startBlock) {
		for (final BlockNode pred : startBlock.getPredecessors()) {
			if (pred.isEmpty()) {
				if (handlerBlocks.remove(pred) && !BlockUtils.isBackEdge(pred, startBlock)) {
					removeEmptyUpPath(handlerBlocks, pred);
				}
			}
		}
	}

	private static void copyCodeVars(final InsnNode fromInsn, final InsnNode toInsn) {
		copyCodeVars(fromInsn.getResult(), toInsn.getResult());
		final int argsCount = fromInsn.getArgsCount();
		for (int i = 0; i < argsCount; i++) {
			copyCodeVars(fromInsn.getArg(i), toInsn.getArg(i));
		}
	}

	private static void copyCodeVars(final InsnArg fromArg, final InsnArg toArg) {
		if (fromArg == null || toArg == null
				|| !fromArg.isRegister() || !toArg.isRegister()) {
			return;
		}
		final SSAVar fromSsaVar = ((RegisterArg) fromArg).getSVar();
		final SSAVar toSsaVar = ((RegisterArg) toArg).getSVar();
		toSsaVar.setCodeVar(fromSsaVar.getCodeVar());
	}

	/**
	 * Reload method without applying this visitor
	 */
	private static void undoFinallyVisitor(final MethodNode mth) {
		try {
			// TODO: make more common and less hacky
			mth.unload();
			mth.load();
			for (IDexTreeVisitor visitor : mth.root().getPasses()) {
				if (visitor instanceof MarkFinallyVisitor) {
					break;
					// All visitors after MarkFinally will be invoked as usual after this because
					// the original decompilation request will proceed.
				}
				DepthTraversal.visit(visitor, mth);
			}
		} catch (final DecodeException e) {
			mth.addError("Undo finally extract failed", e);
		}
	}

	@Nullable
	private static BlockNode getTerminusForHandler(final ExceptionHandler handler, final TryExtractInfo tryInfo) {
		for (final BlockNode terminus : tryInfo.scopeTerminusGroups.keySet()) {
			final List<TryEdge> edgesWithTerminus = tryInfo.scopeTerminusGroups.get(terminus);
			for (final TryEdge edge : edgesWithTerminus) {
				if (edge.isNotHandlerExit()) {
					continue;
				}
				if (edge.getExceptionHandler().equals(handler)) {
					return terminus;
				}
			}
		}
		return null;
	}
}
