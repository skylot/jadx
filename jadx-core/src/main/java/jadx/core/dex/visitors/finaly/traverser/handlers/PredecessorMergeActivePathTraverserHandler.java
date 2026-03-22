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

	private static List<BlockNode> orderBlocks(List<BlockNode> blocks) {
		List<BlockNode> dup = new ArrayList<>(blocks);
		// Collections.sort(dup, (blk1, blk2) -> Integer.compare(blk1.getCId(), blk2.getCId()));
		return dup;
	}

	public PredecessorMergeActivePathTraverserHandler(TraverserActivePathState initialState) {
		super(initialState);
	}

	@Override
	protected List<TraverserActivePathState> handle() throws TraverserException {
		// At this point, we expect the handler to contain the block state of the path which is
		// requesting a predecessor merge. If the other handler also requests a predecessor merge,
		// we can merge the two. If not, we'll split the active handler to support the multiple
		// paths.
		TraverserActivePathState comparator = getComparator();
		TraverserState finallyState = comparator.getFinallyState();
		TraverserState candidateState = comparator.getCandidateState();

		boolean finallyNeedsDuplicate = finallyState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE;
		boolean candidateNeedsDuplicate = candidateState.getCompareState() == TraverserState.ComparisonState.READY_TO_COMPARE;
		boolean shouldMerge = finallyNeedsDuplicate && candidateNeedsDuplicate;

		if (shouldMerge) {
			return mergeScopes((UnknownAdvanceStrategyTraverserState) finallyState, (UnknownAdvanceStrategyTraverserState) candidateState);
		} else {
			UnknownAdvanceStrategyTraverserState advancingState;
			TraverserState otherState;
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

	private List<TraverserActivePathState> mergeScopes(UnknownAdvanceStrategyTraverserState finallyState,
			UnknownAdvanceStrategyTraverserState candidateState) throws TraverserException {
		List<BlockNode> finallyBlocks = finallyState.getNextBlocks();
		List<BlockNode> candidateBlocks = candidateState.getNextBlocks();

		int finallyBlocksSize = finallyBlocks.size();
		int candidateBlocksSize = candidateBlocks.size();

		List<TraverserActivePathState> states;
		if (candidateBlocksSize % finallyBlocksSize == 0 && candidateBlocksSize == finallyBlocksSize) {
			List<BlockNode> finallyBlocksOrdered = orderBlocks(finallyBlocks);
			List<BlockNode> candidateBlocksOrdered = orderBlocks(candidateBlocks);

			int duplicationCount = candidateBlocksSize / finallyBlocksSize;

			states = new ArrayList<>(duplicationCount);
			for (int i = 0; i < duplicationCount; i++) {
				List<BlockNode> candidateBlocksSubset = new ArrayList<>(finallyBlocksSize);
				for (int j = 0; j < finallyBlocksSize; j++) {
					candidateBlocksSubset.add(candidateBlocksOrdered.get(i * finallyBlocksSize + j));
				}

				TraverserActivePathState comparatorState = getScopeForBlocks(finallyBlocksOrdered, candidateBlocksSubset);
				states.add(comparatorState);
			}
		} else {
			TraverserStateFactory<TerminalTraverserState> finallyStateFactory =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNMERGEABLE_STATE);
			TraverserStateFactory<TerminalTraverserState> candidateStateFactory =
					TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.UNMERGEABLE_STATE);
			TraverserActivePathState newState =
					TraverserActivePathState.produceFromFactories(getComparator(), finallyStateFactory, candidateStateFactory);
			states = List.of(newState);
		}
		return states;
	}

	private List<TraverserActivePathState> duplicateForPaths(TraverserActivePathState comparator,
			UnknownAdvanceStrategyTraverserState advancingState, TraverserState otherState,
			boolean duplicateIsFromFinally) {
		List<BlockNode> nextPredecessors = advancingState.getNextBlocks();
		List<TraverserActivePathState> newPaths = new ArrayList<>(nextPredecessors.size());
		for (BlockNode predecessor : nextPredecessors) {
			CentralityState centralityState = advancingState.getCentralityState();
			TraverserBlockInfo duplicatePathBlockInfo = new TraverserBlockInfo(predecessor);
			TraverserStateFactory<NewBlockTraverserState> duplicatePathStateFactory =
					NewBlockTraverserState.getFactory(centralityState, duplicatePathBlockInfo);
			TraverserStateFactory<?> otherStateFactory = new DuplicatedTraverserStateFactory<>(otherState);

			TraverserActivePathState comparatorDuplicated = comparator.duplicate();
			TraverserActivePathState newPathState;
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

	private TraverserActivePathState getScopeForBlocks(List<BlockNode> finallyBlocks, List<BlockNode> candidateBlocks) {
		TraverserActivePathState comparator = getComparator();
		MethodNode mth = getComparator().getGlobalCommonState().getMethodNode();

		TraverserState finallyState = comparator.getFinallyState();
		TraverserState candidateState = comparator.getCandidateState();

		GlobalTraverserSourceState finallyGlobalState = comparator.getGlobalStateFor(finallyState);
		CentralityState finallyCentralityState = finallyState.getCentralityState();
		BlockNode finallyTerminator =
				BlockUtils.getBottomCommonPredecessor(mth, finallyBlocks, finallyGlobalState.getContainedBlocks());
		TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> finallyStateFactory =
				IdentifiedScopeWithTerminatorTraverserState.getFactory(finallyCentralityState, finallyBlocks, finallyTerminator);

		GlobalTraverserSourceState candidateGlobalState = comparator.getGlobalStateFor(candidateState);
		CentralityState candidateCentralityState = candidateState.getCentralityState();
		BlockNode candidateTerminator =
				BlockUtils.getBottomCommonPredecessor(mth, candidateBlocks, candidateGlobalState.getContainedBlocks());
		TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> candidateStateFactory =
				IdentifiedScopeWithTerminatorTraverserState.getFactory(candidateCentralityState, candidateBlocks, candidateTerminator);

		return TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
	}
}
