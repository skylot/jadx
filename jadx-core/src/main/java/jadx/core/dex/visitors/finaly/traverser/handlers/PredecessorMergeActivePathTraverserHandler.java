package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.GlobalTraverserSourceState;
import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.factory.DuplicatedTraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.state.IdentifiedScopeWithTerminatorTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.NewBlockTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TerminalTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.UnknownAdvanceStrategyTraverserState;
import jadx.core.utils.BlockUtils;

public final class PredecessorMergeActivePathTraverserHandler extends AbstractActivePathTraverserHandler {

	private static List<BlockNode> orderBlocks(final List<BlockNode> blocks) {
		final List<BlockNode> dup = new ArrayList<>(blocks);

		// Collections.sort(dup, (blk1, blk2) -> Integer.compare(blk1.getCId(), blk2.getCId()));

		return dup;
	}

	public PredecessorMergeActivePathTraverserHandler(TraverserActivePathState initialState) {
		super(initialState);
	}

	@Override
	protected final List<TraverserActivePathState> handle() throws TraverserException {
		// At this point, we expect the handler to contain the block state of the path which is
		// requesting a predecessor merge. If the other handler also requests a predecessor merge,
		// we can merge the two. If not, we'll split the active handler to support the multiple
		// paths.

		final TraverserActivePathState comparator = getComparator();
		final TraverserState finallyState = comparator.getFinallyState();
		final TraverserState candidateState = comparator.getCandidateState();

		final boolean finallyNeedsDuplicate = finallyState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE;
		final boolean candidateNeedsDuplicate = candidateState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE;
		final boolean shouldMerge = finallyNeedsDuplicate && candidateNeedsDuplicate;

		if (shouldMerge) {
			return mergeScopes((UnknownAdvanceStrategyTraverserState) finallyState, (UnknownAdvanceStrategyTraverserState) candidateState);
		} else {
			final UnknownAdvanceStrategyTraverserState advancingState;
			final TraverserState otherState;
			if (finallyNeedsDuplicate) {
				advancingState = (UnknownAdvanceStrategyTraverserState) finallyState;
				otherState = candidateState;
			} else {
				advancingState = (UnknownAdvanceStrategyTraverserState) candidateState;
				otherState = finallyState;
			}
			return duplicateForPaths(comparator, advancingState, otherState, finallyNeedsDuplicate);
		}
	}

	private List<TraverserActivePathState> mergeScopes(final UnknownAdvanceStrategyTraverserState finallyState,
			final UnknownAdvanceStrategyTraverserState candidateState) throws TraverserException {
		final List<BlockNode> finallyBlocks = finallyState.getNextBlocks();
		final List<BlockNode> candidateBlocks = candidateState.getNextBlocks();

		final int finallyBlocksSize = finallyBlocks.size();
		final int candidateBlocksSize = candidateBlocks.size();

		final List<TraverserActivePathState> states;
		if (candidateBlocksSize % finallyBlocksSize == 0 && candidateBlocksSize == finallyBlocksSize) {
			final List<BlockNode> finallyBlocksOrdered = orderBlocks(finallyBlocks);
			final List<BlockNode> candidateBlocksOrdered = orderBlocks(candidateBlocks);

			final int duplicationCount = candidateBlocksSize / finallyBlocksSize;

			states = new ArrayList<>(duplicationCount);
			for (int i = 0; i < duplicationCount; i++) {
				final List<BlockNode> candidateBlocksSubset = new ArrayList<>(finallyBlocksSize);
				for (int j = 0; j < finallyBlocksSize; j++) {
					candidateBlocksSubset.add(candidateBlocksOrdered.get(i * finallyBlocksSize + j));
				}

				final TraverserActivePathState comparatorState = getScopeForBlocks(finallyBlocksOrdered, candidateBlocksSubset);
				states.add(comparatorState);
			}
		} else {
			final TraverserStateFactory<TerminalTraverserState> finallyStateFactory =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNMERGEABLE_STATE);
			final TraverserStateFactory<TerminalTraverserState> candidateStateFactory =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNMERGEABLE_STATE);
			final TraverserActivePathState newState =
					TraverserActivePathState.produceFromFactories(getComparator(), finallyStateFactory, candidateStateFactory);
			states = List.of(newState);
		}
		return states;
	}

	private List<TraverserActivePathState> duplicateForPaths(final TraverserActivePathState comparator,
			final UnknownAdvanceStrategyTraverserState advancingState, final TraverserState otherState,
			final boolean duplicateIsFromFinally) {
		final List<BlockNode> nextPredecessors = advancingState.getNextBlocks();
		final List<TraverserActivePathState> newPaths = new ArrayList<>(nextPredecessors.size());
		for (final BlockNode predecessor : nextPredecessors) {
			final CentralityState centralityState = advancingState.getCentralityState();
			final TraverserBlockInfo duplicatePathBlockInfo = new TraverserBlockInfo(predecessor);
			final TraverserStateFactory<NewBlockTraverserState> duplicatePathStateFactory =
					NewBlockTraverserState.getFactory(centralityState, duplicatePathBlockInfo);
			final TraverserStateFactory<?> otherStateFactory = new DuplicatedTraverserStateFactory<>(otherState);

			final TraverserActivePathState comparatorDuplicated = comparator.duplicate();
			final TraverserActivePathState newPathState;
			if (duplicateIsFromFinally) {
				newPathState =
						TraverserActivePathState.produceFromFactories(comparatorDuplicated, duplicatePathStateFactory, otherStateFactory);
			} else {
				newPathState =
						TraverserActivePathState.produceFromFactories(comparatorDuplicated, otherStateFactory, duplicatePathStateFactory);
			}
			newPaths.add(newPathState);
		}

		return newPaths;
	}

	private TraverserActivePathState getScopeForBlocks(final List<BlockNode> finallyBlocks, final List<BlockNode> candidateBlocks) {
		final TraverserActivePathState comparator = getComparator();
		final MethodNode mth = getComparator().getGlobalCommonState().getMethodNode();

		final TraverserState finallyState = comparator.getFinallyState();
		final TraverserState candidateState = comparator.getCandidateState();

		final GlobalTraverserSourceState finallyGlobalState = comparator.getGlobalStateFor(finallyState);
		final CentralityState finallyCentralityState = finallyState.getCentralityState();
		final BlockNode finallyTerminator =
				BlockUtils.getBottomCommonPredecessor(mth, finallyBlocks, finallyGlobalState.getContainedBlocks());
		final TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> finallyStateFactory =
				IdentifiedScopeWithTerminatorTraverserState.getFactory(finallyCentralityState, finallyBlocks, finallyTerminator);

		final GlobalTraverserSourceState candidateGlobalState = comparator.getGlobalStateFor(candidateState);
		final CentralityState candidateCentralityState = candidateState.getCentralityState();
		final BlockNode candidateTerminator =
				BlockUtils.getBottomCommonPredecessor(mth, candidateBlocks, candidateGlobalState.getContainedBlocks());
		final TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> candidateStateFactory =
				IdentifiedScopeWithTerminatorTraverserState.getFactory(candidateCentralityState, candidateBlocks, candidateTerminator);

		return TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
	}
}
