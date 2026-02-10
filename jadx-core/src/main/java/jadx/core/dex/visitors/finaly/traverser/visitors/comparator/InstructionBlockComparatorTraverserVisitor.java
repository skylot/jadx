package jadx.core.dex.visitors.finaly.traverser.visitors.comparator;

import java.util.ArrayList;
import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.nodes.InsnNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.SameInstructionsStrategy;
import jadx.core.dex.visitors.finaly.SameInstructionsStrategyImpl;
import jadx.core.dex.visitors.finaly.traverser.factory.DuplicatedTraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.state.NoBlockTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TerminalTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.utils.Pair;

public final class InstructionBlockComparatorTraverserVisitor extends AbstractTraverserComparatorVisitor {

	private static TraverserActivePathState createStateForPerfectMatch(final TraverserActivePathState previousState,
			final BlockNode finallyBlock,
			final BlockNode candidateBlock) {
		final CentralityState finallyCentralityState = previousState.getFinallyState().getCentralityState().duplicate();
		final CentralityState candidateCentralityState = previousState.getCandidateState().getCentralityState().duplicate();

		finallyCentralityState.setAllowsCentral(false);
		candidateCentralityState.setAllowsCentral(false);
		finallyCentralityState.setAllowsNonStartingNode(false);
		candidateCentralityState.setAllowsNonStartingNode(false);

		final TraverserStateFactory<NoBlockTraverserState> finallyStateProducer =
				NoBlockTraverserState.getFactory(finallyCentralityState, finallyBlock);
		final TraverserStateFactory<NoBlockTraverserState> candidateStateProducer =
				NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);

		return TraverserActivePathState.produceFromFactories(previousState, finallyStateProducer, candidateStateProducer);
	}

	private static TraverserActivePathState createStateForUnevenMatch(final TraverserActivePathState previousState,
			final TraverserState finallyState,
			final TraverserState candidateState, final BlockNode finallyBlock, final BlockNode candidateBlock, final int finallyInsnsSize,
			final int candidateInsnsSize) {
		final int maxIterateCount = Math.max(finallyInsnsSize, candidateInsnsSize);
		final boolean finallyOverruns = finallyInsnsSize > candidateInsnsSize;

		final int insnsDelta;
		final TraverserStateFactory<?> newFinallyStateProducer;
		final TraverserStateFactory<?> newCandidateStateProducer;
		final TraverserBlockInfo adjustedBlockInfo;
		if (finallyOverruns) {
			// More finally instructions than candidate instructions
			final CentralityState candidateCentralityState = candidateState.getCentralityState().duplicate();
			candidateCentralityState.setAllowsCentral(false);
			candidateCentralityState.setAllowsNonStartingNode(false);
			final CentralityState finallyCentralityState = finallyState.getCentralityState();
			finallyCentralityState.setAllowsCentral(false);
			finallyCentralityState.setAllowsNonStartingNode(false);

			insnsDelta = finallyInsnsSize - maxIterateCount;
			newFinallyStateProducer = new DuplicatedTraverserStateFactory<>(finallyState);
			adjustedBlockInfo = finallyState.getBlockInsnInfo();
			newCandidateStateProducer = NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);
		} else {
			// More candidate instructions than finally instructions
			final CentralityState finallyCentralityState = finallyState.getCentralityState().duplicate();
			finallyCentralityState.setAllowsCentral(false);
			finallyCentralityState.setAllowsNonStartingNode(false);
			final CentralityState candidateCentralityState = candidateState.getCentralityState();
			candidateCentralityState.setAllowsCentral(false);
			candidateCentralityState.setAllowsNonStartingNode(false);

			insnsDelta = candidateInsnsSize - maxIterateCount;
			candidateState.getCentralityState().setAllowsCentral(false);
			newCandidateStateProducer = new DuplicatedTraverserStateFactory<>(candidateState);
			adjustedBlockInfo = candidateState.getBlockInsnInfo();
			newFinallyStateProducer = NoBlockTraverserState.getFactory(finallyCentralityState, finallyBlock);
		}
		adjustedBlockInfo.setBottomOffset(adjustedBlockInfo.getBottomOffset() + insnsDelta);

		return TraverserActivePathState.produceFromFactories(previousState, newFinallyStateProducer, newCandidateStateProducer);
	}

	private static TraverserActivePathState createStateForBlockSkip(final TraverserActivePathState previousState,
			final TraverserState finallyState,
			final TraverserState candidateState, final BlockNode finallyBlock, final BlockNode candidateBlock) {
		final CentralityState finallyCentralityState = finallyState.getCentralityState();
		final CentralityState candidateCentralityState = candidateState.getCentralityState();

		// TODO: Maybe replace this with controller logic so that we can determine if we need to use these
		// as path ends and then merge above path?

		// Fix up finally path first. If this continues to fail, check if candidate can be fixed up in a
		// later iteration.
		if (finallyCentralityState.getAllowsNonStartingNode()) {
			finallyCentralityState.setAllowsNonStartingNode(false);
			final TraverserStateFactory<NoBlockTraverserState> newFinallyStateProducer =
					NoBlockTraverserState.getFactory(finallyCentralityState, finallyBlock);
			final TraverserStateFactory<?> newCandidateStateProducer = new DuplicatedTraverserStateFactory<>(candidateState);
			return TraverserActivePathState.produceFromFactories(previousState, newFinallyStateProducer, newCandidateStateProducer);
		} else {
			candidateCentralityState.setAllowsNonStartingNode(false);
			final TraverserStateFactory<NoBlockTraverserState> newCandidateStateProducer =
					NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);
			final TraverserStateFactory<?> newFinallyStateProducer = new DuplicatedTraverserStateFactory<>(finallyState);
			return TraverserActivePathState.produceFromFactories(previousState, newFinallyStateProducer, newCandidateStateProducer);
		}
	}

	private static TraverserActivePathState createStateForTerminatorState(final TraverserActivePathState previousState) {
		final TraverserStateFactory<TerminalTraverserState> finallyStateProducer =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_INSTRUCTIONS);
		final TraverserStateFactory<TerminalTraverserState> candidateStateProducer =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_INSTRUCTIONS);

		return TraverserActivePathState.produceFromFactories(previousState, finallyStateProducer, candidateStateProducer);
	}

	private final SameInstructionsStrategy sameInstructionsStrategy = new SameInstructionsStrategyImpl();

	@Override
	public final TraverserActivePathState visit(final TraverserActivePathState state) {
		final TraverserState finallyState = state.getFinallyState();
		final TraverserState candidateState = state.getCandidateState();

		final TraverserBlockInfo finallyBlockInfo = finallyState.getBlockInsnInfo();
		final TraverserBlockInfo candidateBlockInfo = candidateState.getBlockInsnInfo();

		if (finallyBlockInfo == null || candidateBlockInfo == null) {
			throw new UnsupportedOperationException(
					"The instruction comparator handler has received a state which does not support block insn info");
		}

		final BlockNode finallyBlock = finallyBlockInfo.getBlock();
		final BlockNode candidateBlock = candidateBlockInfo.getBlock();

		final List<InsnNode> finallyInsns = finallyBlockInfo.getInsnsSlice();
		final List<InsnNode> candidateInsns = candidateBlockInfo.getInsnsSlice();
		final int finallyInsnsSize = finallyInsns.size();
		final int candidateInsnsSize = candidateInsns.size();

		final int maxIterateCount = Math.min(finallyInsnsSize, candidateInsnsSize);

		final List<Pair<InsnNode>> matchingInsns = new ArrayList<>(maxIterateCount);

		// Search through each instruction in reverse and see how many match
		for (int i = 0; i < maxIterateCount; i++) {
			final InsnNode candidateInsn = candidateInsns.get(candidateInsnsSize - i - 1);
			final InsnNode finallyInsn = finallyInsns.get(finallyInsnsSize - i - 1);

			if (!sameInstructionsStrategy.sameInsns(candidateInsn, finallyInsn)) {
				break;
			}

			final Pair<InsnNode> match = new Pair<>(finallyInsn, candidateInsn);
			matchingInsns.add(match);
		}

		final int matchedInsnsCount = matchingInsns.size();

		state.registerWithBlockInfo(finallyBlockInfo, matchedInsnsCount);
		state.registerWithBlockInfo(candidateBlockInfo, matchedInsnsCount);

		final boolean finallyOverruns = finallyInsnsSize > candidateInsnsSize;
		final boolean candidateOverruns = finallyInsnsSize < candidateInsnsSize;
		final boolean sameSizedSlices = !finallyOverruns && !candidateOverruns;
		final boolean allMatched = matchedInsnsCount == maxIterateCount;
		final boolean noneMatched = matchedInsnsCount == 0;

		state.getMatchedInsns().addAll(matchingInsns);

		final TraverserActivePathState newState;
		if (allMatched) {
			if (sameSizedSlices) {
				// All instructions matched and there are no more instructions to match in either
				// block. Continue to the next set of blocks.
				newState = createStateForPerfectMatch(state, finallyBlock, candidateBlock);
			} else {
				// All instructions matched, however one block contained more instructions than the
				// other. Continue to next set of blocks for the handler whose instructions list was
				// fully searched.
				newState = createStateForUnevenMatch(state, finallyState, candidateState, finallyBlock, candidateBlock, finallyInsnsSize,
						candidateInsnsSize);
			}
		} else if (noneMatched && eitherStateAllowsBlockSkip(finallyState, candidateState)) {
			newState = createStateForBlockSkip(state, finallyState, candidateState, finallyBlock, candidateBlock);
		} else {
			// If any didn't match, this means that the first instructions of the block don't
			// match. This therefore means that no future blocks should be marked as duplicate
			// instructions and thus we should return a terminator state to stop the search.
			newState = createStateForTerminatorState(state);
		}

		return newState;
	}

	private boolean eitherStateAllowsBlockSkip(final TraverserState finallyState, final TraverserState candidateState) {
		final CentralityState finallyCentralityState = finallyState.getCentralityState();
		final CentralityState candidateCentralityState = candidateState.getCentralityState();

		return finallyCentralityState.getAllowsNonStartingNode() || candidateCentralityState.getAllowsNonStartingNode();
	}
}
