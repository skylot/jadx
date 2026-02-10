package jadx.core.dex.visitors.finaly.traverser;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractActivePathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockPathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.state.RecoveredFromCacheTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TerminalTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserGlobalCommonState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Responsible for determining if two distinct subgraphs are the same within a graph by comparing
 * all blocks and their instructions.
 * This is used for identifying duplicated instructions for extracting finally blocks.
 *
 * The terms "finally" and "candidate" are used to represent the two distinct subgraphs explored
 * by this controller; the "finally" subgraph, which is the subgraph which is what is being used
 * as a finally block, and the "candidate" subgraph, which is the subgraph which is being
 * compared to the "finally" subgraph to see if they are the same. There is only ever one
 * "finally" subgraph, however it is run against multiple different "candidate" subgraphs depending
 * on the complexity of the try catch block that this is being run for.
 */
public final class TraverserController {

	private static List<TraverserActivePathState> processHandlerImplementations(final TraverserActivePathState state,
			final AbstractBlockTraverserHandler handler) throws TraverserException {
		if (handler instanceof AbstractBlockPathTraverserHandler) {
			((AbstractBlockPathTraverserHandler) handler).process();
			return List.of(state);
		} else if (handler instanceof AbstractActivePathTraverserHandler) {
			return ((AbstractActivePathTraverserHandler) handler).process();
		} else {
			throw new JadxRuntimeException(
					"A sealed class, " + AbstractBlockPathTraverserHandler.class.getSimpleName() + ", has an unknown implementation");
		}
	}

	private final @Nullable Function<TraverserState, Boolean> stateAbortCondition;

	public TraverserController() {
		this(null);
	}

	public TraverserController(final @Nullable Function<TraverserState, Boolean> stateAbortCondition) {
		this.stateAbortCondition = stateAbortCondition;
	}

	/**
	 * Processes a traverser path state using from a {@link TraverserActivePathState}. This
	 * function will continue evaluating an active path until either:
	 * <ul>
	 * <li>The state abort condition is met by both "finally" and "candidate" path, if there is
	 * one.</li>
	 * <li>The path state of either the "finally" or "candidate" path has terminated.</li>
	 * <li>The path has began a comparison of two blocks which have already been compared.</li>
	 * <li>The "finally" and "candidate" states, on two different executions of
	 * {@link TraverserController#advance}, did not change.
	 * </ul>
	 * This function will return a list of all of the different paths taken at the point of
	 * termination of each individual branch.
	 *
	 * @param state
	 * @return
	 */
	public final List<TraverserActivePathState> process(final TraverserActivePathState state) throws TraverserException {
		TraverserActivePathState nextState = state;
		final AtomicReference<TraverserState> previousFinallyState = new AtomicReference<>(null);
		final AtomicReference<TraverserState> previousCandidateState = new AtomicReference<>(null);
		while (true) {
			final List<TraverserActivePathState> advancedStates = advance(nextState, previousFinallyState, previousCandidateState);
			if (advancedStates == null || advancedStates.isEmpty()) {
				break;
			}

			if (advancedStates.size() != 1) {
				final TraverserController nextController = new TraverserController(stateAbortCondition);
				final List<TraverserActivePathState> returnStates = new ArrayList<>();
				for (final TraverserActivePathState advancedState : advancedStates) {
					final List<TraverserActivePathState> childStates = nextController.process(advancedState);
					returnStates.addAll(childStates);
				}
				return returnStates;
			}

			nextState = advancedStates.get(0);
		}
		return List.of(nextState);
	}

	/**
	 * Processes a singular traverser state once.
	 *
	 * @param state
	 * @param previousFinallyState
	 * @param previousCandidateState
	 * @return
	 */
	public final List<TraverserActivePathState> advance(final TraverserActivePathState state,
			final AtomicReference<TraverserState> previousFinallyState,
			final AtomicReference<TraverserState> previousCandidateState) throws TraverserException {
		final TraverserGlobalCommonState commonState = state.getGlobalCommonState();
		final TraverserState finallyState = state.getFinallyState();
		final TraverserState candidateState = state.getCandidateState();

		if (previousFinallyState.get() == finallyState && previousCandidateState.get() == candidateState) {
			final TraverserStateFactory<TerminalTraverserState> finallyStateProducer =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNRESOLVABLE_STATES);
			final TraverserStateFactory<TerminalTraverserState> candidateStateProducer =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNRESOLVABLE_STATES);
			return List.of(TraverserActivePathState.produceFromFactories(state, finallyStateProducer, candidateStateProducer));
		}

		previousFinallyState.set(finallyState);
		previousCandidateState.set(candidateState);

		if (finallyState.isTerminal() || candidateState.isTerminal()) {
			return null;
		}

		if (finallyState.getClass().equals(candidateState.getClass())
				&& finallyState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE
				&& candidateState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE) {

			final BlockNode finallyBlock;
			final BlockNode candidateBlock;
			final TraverserBlockInfo finallyBlockInfo = finallyState.getBlockInsnInfo();
			final TraverserBlockInfo candidateBlockInfo = candidateState.getBlockInsnInfo();
			if (finallyBlockInfo != null && candidateBlockInfo != null) {
				finallyBlock = finallyBlockInfo.getBlock();
				candidateBlock = candidateBlockInfo.getBlock();
			} else {
				finallyBlock = null;
				candidateBlock = null;
			}

			final boolean isCached;
			if (finallyBlock != null && candidateBlock != null) {
				isCached = commonState.hasBlocksBeenCached(finallyBlock, candidateBlock);
			} else {
				isCached = false;
			}

			if (isCached) {
				final List<TraverserActivePathState> dupStates = commonState.getCachedStateFor(finallyBlock, candidateBlock);
				final List<TraverserActivePathState> recoveredFromCacheStates = new ArrayList<>(dupStates.size());
				for (final TraverserActivePathState dupState : dupStates) {
					final TraverserState reusedFinallyState = dupState.getFinallyState();
					final TraverserState reusedCandidateState = dupState.getCandidateState();
					final TraverserStateFactory<?> finallyStateProducer = RecoveredFromCacheTraverserState.getFactory(reusedFinallyState);
					final TraverserStateFactory<?> candidateStateProducer =
							RecoveredFromCacheTraverserState.getFactory(reusedCandidateState);
					final TraverserActivePathState recoveredFromCacheState =
							TraverserActivePathState.produceFromFactories(state, finallyStateProducer, candidateStateProducer);
					recoveredFromCacheState.mergeWith(dupStates);
					recoveredFromCacheStates.add(recoveredFromCacheState);
				}
				return recoveredFromCacheStates;
			}

			final AbstractBlockTraverserHandler handler = candidateState.getNextHandler();
			final List<TraverserActivePathState> resultingStates = processHandlerImplementations(state, handler);
			return resultingStates;
		}

		final boolean hasReadyToCompare = finallyState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE
				|| candidateState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE;

		final boolean finallyStateAborted = advanceSingleState(state, finallyState, hasReadyToCompare);
		final boolean candidateStateAborted = advanceSingleState(state, candidateState, hasReadyToCompare);

		if (finallyStateAborted && candidateStateAborted) {
			return null;
		}

		return List.of(state);
	}

	/**
	 * Advances a singular state once.
	 *
	 * @return Whether this state has been aborted by the state abort function.
	 */
	private boolean advanceSingleState(final TraverserActivePathState activePathState, final TraverserState singleState,
			final boolean hasReadyToCompare) throws TraverserException {
		final boolean stateAborted = stateAbortCondition != null && stateAbortCondition.apply(singleState);
		if (stateAbortCondition == null || !stateAborted) {
			if (singleState.getCompareState() == TraverserState.ComparisonState.NOT_READY
					|| (singleState.getCompareState() == TraverserState.ComparisonState.AWAITING_OPTIONAL_PREDECESSOR_MERGE
							&& hasReadyToCompare)) {
				final AbstractBlockTraverserHandler handler = singleState.getNextHandler();
				final List<TraverserActivePathState> results = processHandlerImplementations(activePathState, handler);
				if (results.size() != 1 || results.get(0) != activePathState) {
					throw new JadxRuntimeException("A traverser handler which was not expected to change path states actually did");
				}
			}
		}
		return stateAborted;
	}
}
