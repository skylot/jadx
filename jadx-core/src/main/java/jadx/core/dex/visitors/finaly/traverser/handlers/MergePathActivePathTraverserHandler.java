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

	private static TraverserActivePathState createNonMatchingTerminator(final TraverserActivePathState state) {
		final TraverserStateFactory<TerminalTraverserState> finallyStateFactory =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_PATHS);
		final TraverserStateFactory<TerminalTraverserState> candidateStateFactory =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_PATHS);

		return TraverserActivePathState.produceFromFactories(state, finallyStateFactory, candidateStateFactory);
	}

	private static boolean isStateOnTerminus(final TraverserState state, final BlockNode terminus) {
		final TraverserBlockInfo blockInfo = state.getBlockInsnInfo();
		if (blockInfo == null) {
			return false;
		}
		return blockInfo.getBlock() == terminus;
	}

	private static Function<TraverserState, Boolean> getStateAbortOnTerminusFunction(
			final IdentifiedScopeWithTerminatorTraverserState finallyState,
			final IdentifiedScopeWithTerminatorTraverserState candidateState) {
		final BlockNode finallyTerminus = finallyState.getTerminus();
		final BlockNode candidateTerminus = candidateState.getTerminus();
		final GlobalTraverserSourceState finallyGlobalState = finallyState.getGlobalState();
		final GlobalTraverserSourceState candidateGlobalState = candidateState.getGlobalState();

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

	private static PostMergeStatus getScopeSplitPostMergeStatus(final List<TraverserActivePathState> pathsTaken) {
		// If the scope split is the same, all branches must not end in a terminator.

		final PostMergeStatus status = new PostMergeStatus();
		for (final TraverserActivePathState path : pathsTaken) {
			final TraverserState finallyState;
			final TraverserState candidateState;
			if (path.getFinallyState().isTerminal() || path.getCandidateState().isTerminal()) {
				final TraverserState rawFinallyState = path.getFinallyState();
				final TraverserState rawCandidateState = path.getCandidateState();
				final boolean finallyIsCached = rawFinallyState instanceof RecoveredFromCacheTraverserState;
				final boolean candidateIsCached = rawCandidateState instanceof RecoveredFromCacheTraverserState;
				if (!(finallyIsCached && candidateIsCached)) {
					status.perfectMatch = false;
					continue;
				}

				final RecoveredFromCacheTraverserState finallyCachedState = (RecoveredFromCacheTraverserState) rawFinallyState;
				final RecoveredFromCacheTraverserState candidateCachedState = (RecoveredFromCacheTraverserState) rawCandidateState;
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

			final CentralityState finallyCentralityState = finallyState.getCentralityState();
			final CentralityState candidateCentralityState = candidateState.getCentralityState();
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

	public MergePathActivePathTraverserHandler(final TraverserActivePathState comparatorState) {
		super(comparatorState);
	}

	@Override
	protected final List<TraverserActivePathState> handle() {
		final TraverserActivePathState comparator = getComparator().duplicate();
		final TraverserGlobalCommonState commonState = comparator.getGlobalCommonState();
		final IdentifiedScopeWithTerminatorTraverserState finallyState =
				(IdentifiedScopeWithTerminatorTraverserState) comparator.getFinallyState();
		final IdentifiedScopeWithTerminatorTraverserState candidateState =
				(IdentifiedScopeWithTerminatorTraverserState) comparator.getCandidateState();

		final BlockNode finallyTerminus = finallyState.getTerminus();
		final BlockNode candidateTerminus = candidateState.getTerminus();

		final Function<TraverserState, Boolean> abortFunction = getStateAbortOnTerminusFunction(finallyState, candidateState);

		final List<BlockNode[]> allPermutationsPaths = getAllPermutationsOfCollection(candidateState.getRoots());
		List<TraverserActivePathState> paths = null;
		PostMergeStatus postMerge = null;
		for (final BlockNode[] candidateRootsPermutation : allPermutationsPaths) {
			final List<TraverserActivePathState> traversalPaths = new ArrayList<>();
			for (int i = 0; i < finallyState.getRoots().size(); i++) {
				final var finallyRoot = finallyState.getRoots().get(i);
				final var candidateRoot = candidateRootsPermutation[i];

				final var finallyCentrality = finallyState.getCentralityState().duplicate();
				final var candidateCentrality = candidateState.getCentralityState().duplicate();

				final var finallyBlockInfo = new TraverserBlockInfo(finallyRoot);
				final var candidateBlockInfo = new TraverserBlockInfo(candidateRoot);

				final var finallyStateFactory = NewBlockTraverserState.getFactory(finallyCentrality, finallyBlockInfo);
				final var candidateStateFactory = NewBlockTraverserState.getFactory(candidateCentrality, candidateBlockInfo);

				final var newState = TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
				traversalPaths.add(newState);
			}

			final List<TraverserActivePathState> currentPaths = new ArrayList<>();
			boolean errorOccurred = false;
			for (final TraverserActivePathState pathState : traversalPaths) {
				final TraverserController branchController = new TraverserController(abortFunction);
				final List<TraverserActivePathState> out;
				try {
					out = branchController.process(pathState);
				} catch (final TraverserException e) {
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
			final boolean hasTerminusBeenEvaluatedInPaths = commonState.hasBlocksBeenCached(finallyTerminus, candidateTerminus);
			final PostMergeStatus currentPostMerge = getScopeSplitPostMergeStatus(currentPaths);
			if (!currentPostMerge.perfectMatch && !hasTerminusBeenEvaluatedInPaths) {
				// No match
				continue;
			}

			paths = currentPaths;
			postMerge = currentPostMerge;
			break;
		}

		if (paths == null || postMerge == null) {
			final TraverserActivePathState nonMatchingState = createNonMatchingTerminator(comparator);
			return List.of(nonMatchingState);
		}

		final CentralityState newFinallyCentralityState = finallyState.getCentralityState().duplicate();
		newFinallyCentralityState.setAllowsCentral(postMerge.finallyAllowsCentral);
		newFinallyCentralityState.addAllowableOutputs(postMerge.finallyAllowableOutputs);
		final CentralityState newCandidateCentralityState = candidateState.getCentralityState().duplicate();
		newCandidateCentralityState.setAllowsCentral(postMerge.candidateAllowsCentral);
		newCandidateCentralityState.addAllowableOutputs(postMerge.candidateAllowableOutputs);

		final TraverserBlockInfo finallyTerminusBlockInfo = new TraverserBlockInfo(finallyState.getTerminus());
		final TraverserBlockInfo candidateTerminusBlockInfo = new TraverserBlockInfo(candidateState.getTerminus());

		final TraverserStateFactory<NewBlockTraverserState> finallyStateFactory =
				NewBlockTraverserState.getFactory(newFinallyCentralityState, finallyTerminusBlockInfo);
		final TraverserStateFactory<NewBlockTraverserState> candidateStateFactory =
				NewBlockTraverserState.getFactory(newCandidateCentralityState, candidateTerminusBlockInfo);

		final TraverserActivePathState nextState =
				TraverserActivePathState.produceFromFactories(comparator, finallyStateFactory, candidateStateFactory);
		nextState.mergeWith(paths);
		return List.of(nextState);
	}

	public static List<BlockNode[]> getAllPermutationsOfCollection(final Collection<BlockNode> elements) {
		final Stack<BlockNode> permutationStack = new Stack<>();
		final List<BlockNode[]> permutations = new ArrayList<>();
		permutations(permutations, elements, permutationStack, elements.size());
		return permutations;
	}

	public static void permutations(final List<BlockNode[]> permutations, final Collection<BlockNode> elements,
			final Stack<BlockNode> permutationStack, final int size) {
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
