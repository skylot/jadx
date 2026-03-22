package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.function.Function;

import jadx.core.dex.instructions.args.RegisterArg;
import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.GlobalTraverserSourceState;
import jadx.core.dex.visitors.finaly.traverser.TraverserController;
import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.state.IdentifiedScopeWithTerminatorTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.NewBlockTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.RecoveredFromCacheTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TerminalTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserGlobalCommonState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class MergePathActivePathTraverserHandler extends AbstractActivePathTraverserHandler {

	private static TraverserActivePathState createNonMatchingTerminator(TraverserActivePathState state) {
		TraverserStateFactory<TerminalTraverserState> finallyStateFactory =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_PATHS);
		TraverserStateFactory<TerminalTraverserState> candidateStateFactory =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_PATHS);

		return TraverserActivePathState.produceFromFactories(state, finallyStateFactory, candidateStateFactory);
	}

	private static boolean isStateOnTerminus(TraverserState state, BlockNode terminus) {
		TraverserBlockInfo blockInfo = state.getBlockInsnInfo();
		if (blockInfo == null) {
			return false;
		}
		return blockInfo.getBlock() == terminus;
	}

	private static Function<TraverserState, Boolean> getStateAbortOnTerminusFunction(
			IdentifiedScopeWithTerminatorTraverserState finallyState,
			IdentifiedScopeWithTerminatorTraverserState candidateState) {
		BlockNode finallyTerminus = finallyState.getTerminus();
		BlockNode candidateTerminus = candidateState.getTerminus();
		GlobalTraverserSourceState finallyGlobalState = finallyState.getGlobalState();
		GlobalTraverserSourceState candidateGlobalState = candidateState.getGlobalState();

		return (final TraverserState state) -> {
			if (state.getGlobalState() == finallyGlobalState) {
				return isStateOnTerminus(state, finallyTerminus);
			} else if (state.getGlobalState() == candidateGlobalState) {
				return isStateOnTerminus(state, candidateTerminus);
			} else {
				throw new JadxRuntimeException("Unknown global traverser state. Has a global state been duplicated?");
			}
		};
	}

	private static PostMergeStatus getScopeSplitPostMergeStatus(List<TraverserActivePathState> pathsTaken) {
		// If the scope split is the same, all branches must not end in a terminator.
		PostMergeStatus status = new PostMergeStatus();
		for (TraverserActivePathState path : pathsTaken) {
			TraverserState finallyState;
			TraverserState candidateState;
			if (path.getFinallyState().isTerminal() || path.getCandidateState().isTerminal()) {
				TraverserState rawFinallyState = path.getFinallyState();
				TraverserState rawCandidateState = path.getCandidateState();
				boolean finallyIsCached = rawFinallyState instanceof RecoveredFromCacheTraverserState;
				boolean candidateIsCached = rawCandidateState instanceof RecoveredFromCacheTraverserState;
				if (!(finallyIsCached && candidateIsCached)) {
					status.perfectMatch = false;
					continue;
				}

				RecoveredFromCacheTraverserState finallyCachedState = (RecoveredFromCacheTraverserState) rawFinallyState;
				RecoveredFromCacheTraverserState candidateCachedState = (RecoveredFromCacheTraverserState) rawCandidateState;
				if (finallyCachedState.canContinue() || candidateCachedState.canContinue()) {
					status.perfectMatch = false;
					continue;
				}
				finallyState = finallyCachedState.getUnderlying();
				candidateState = candidateCachedState.getUnderlying();
			} else {
				finallyState = path.getFinallyState();
				candidateState = path.getCandidateState();
			}
			CentralityState finallyCentralityState = finallyState.getCentralityState();
			CentralityState candidateCentralityState = candidateState.getCentralityState();
			status.finallyAllowsCentral &= finallyCentralityState.getAllowsCentral();
			status.candidateAllowsCentral &= candidateCentralityState.getAllowsCentral();
			status.finallyAllowableOutputs.addAll(finallyCentralityState.getAllowableOutputArguments());
			status.candidateAllowableOutputs.addAll(candidateCentralityState.getAllowableOutputArguments());
		}
		return status;
	}

	private static final class PostMergeStatus {
		public final Set<RegisterArg> finallyAllowableOutputs = new HashSet<>();
		public final Set<RegisterArg> candidateAllowableOutputs = new HashSet<>();
		public boolean finallyAllowsCentral;
		public boolean candidateAllowsCentral;
		public boolean perfectMatch = true;
	}

	public MergePathActivePathTraverserHandler(TraverserActivePathState comparatorState) {
		super(comparatorState);
	}

	@Override
	protected List<TraverserActivePathState> handle() {
		TraverserActivePathState comparator = getComparator().duplicate();
		TraverserGlobalCommonState commonState = comparator.getGlobalCommonState();
		IdentifiedScopeWithTerminatorTraverserState finallyState =
				(IdentifiedScopeWithTerminatorTraverserState) comparator.getFinallyState();
		IdentifiedScopeWithTerminatorTraverserState candidateState =
				(IdentifiedScopeWithTerminatorTraverserState) comparator.getCandidateState();

		BlockNode finallyTerminus = finallyState.getTerminus();
		BlockNode candidateTerminus = candidateState.getTerminus();

		Function<TraverserState, Boolean> abortFunction = getStateAbortOnTerminusFunction(finallyState, candidateState);

		List<BlockNode[]> allPermutationsPaths = getAllPermutationsOfCollection(candidateState.getRoots());
		List<TraverserActivePathState> paths = null;
		PostMergeStatus postMerge = null;
		for (BlockNode[] candidateRootsPermutation : allPermutationsPaths) {
			List<TraverserActivePathState> traversalPaths = new ArrayList<>();
			for (int i = 0; i < finallyState.getRoots().size(); i++) {
				var finallyRoot = finallyState.getRoots().get(i);
				var candidateRoot = candidateRootsPermutation[i];

				var finallyCentrality = finallyState.getCentralityState().duplicate();
				var candidateCentrality = candidateState.getCentralityState().duplicate();

				var finallyBlockInfo = new TraverserBlockInfo(finallyRoot);
				var candidateBlockInfo = new TraverserBlockInfo(candidateRoot);

				var finallyStateFactory = NewBlockTraverserState.getFactory(finallyCentrality, finallyBlockInfo);
				var candidateStateFactory = NewBlockTraverserState.getFactory(candidateCentrality, candidateBlockInfo);

				var newState = TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
				traversalPaths.add(newState);
			}

			List<TraverserActivePathState> currentPaths = new ArrayList<>();
			boolean errorOccurred = false;
			for (TraverserActivePathState pathState : traversalPaths) {
				TraverserController branchController = new TraverserController(abortFunction);
				List<TraverserActivePathState> out;
				try {
					out = branchController.process(pathState);
				} catch (TraverserException e) {
					errorOccurred = true;
					break;
				}
				currentPaths.addAll(out);
			}

			if (errorOccurred) {
				// If an error occurred, this path was not successful.
				continue;
			}

			// If the finally terminus and candidate terminus have been cached at this stage, it means that a
			// path that we searched evaluated the two termini. At this point, we can ignore a non-perfect
			// match if the path could continue from the point of the termini.
			boolean hasTerminusBeenEvaluatedInPaths = commonState.hasBlocksBeenCached(finallyTerminus, candidateTerminus);
			PostMergeStatus currentPostMerge = getScopeSplitPostMergeStatus(currentPaths);
			if (!currentPostMerge.perfectMatch && !hasTerminusBeenEvaluatedInPaths) {
				// No match
				continue;
			}
			paths = currentPaths;
			postMerge = currentPostMerge;
			break;
		}
		if (paths == null || postMerge == null) {
			TraverserActivePathState nonMatchingState = createNonMatchingTerminator(comparator);
			return List.of(nonMatchingState);
		}
		CentralityState newFinallyCentralityState = finallyState.getCentralityState().duplicate();
		newFinallyCentralityState.setAllowsCentral(postMerge.finallyAllowsCentral);
		newFinallyCentralityState.addAllowableOutputs(postMerge.finallyAllowableOutputs);
		CentralityState newCandidateCentralityState = candidateState.getCentralityState().duplicate();
		newCandidateCentralityState.setAllowsCentral(postMerge.candidateAllowsCentral);
		newCandidateCentralityState.addAllowableOutputs(postMerge.candidateAllowableOutputs);

		TraverserBlockInfo finallyTerminusBlockInfo = new TraverserBlockInfo(finallyState.getTerminus());
		TraverserBlockInfo candidateTerminusBlockInfo = new TraverserBlockInfo(candidateState.getTerminus());

		TraverserStateFactory<NewBlockTraverserState> finallyStateFactory =
				NewBlockTraverserState.getFactory(newFinallyCentralityState, finallyTerminusBlockInfo);
		TraverserStateFactory<NewBlockTraverserState> candidateStateFactory =
				NewBlockTraverserState.getFactory(newCandidateCentralityState, candidateTerminusBlockInfo);

		TraverserActivePathState nextState =
				TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
		nextState.mergeWith(paths);
		return List.of(nextState);
	}

	public static List<BlockNode[]> getAllPermutationsOfCollection(Collection<BlockNode> elements) {
		Stack<BlockNode> permutationStack = new Stack<>();
		List<BlockNode[]> permutations = new ArrayList<>();
		permutations(permutations, elements, permutationStack, elements.size());
		return permutations;
	}

	public static void permutations(List<BlockNode[]> permutations, Collection<BlockNode> elements,
			Stack<BlockNode> permutationStack, int size) {
		if (permutationStack.size() == size) {
			permutations.add(permutationStack.toArray(BlockNode[]::new));
		}
		BlockNode[] availableItems = elements.toArray(BlockNode[]::new);
		for (BlockNode i : availableItems) {
			permutationStack.push(i);
			elements.remove(i);
			permutations(permutations, elements, permutationStack, size);
			elements.add(permutationStack.pop());
		}
	}
}
