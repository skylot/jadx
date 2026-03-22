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

	private static TraverserActivePathState createStateForPerfectMatch(TraverserActivePathState previousState,
			BlockNode finallyBlock,
			BlockNode candidateBlock) {
		CentralityState finallyCentralityState = previousState.getFinallyState().getCentralityState().duplicate();
		CentralityState candidateCentralityState = previousState.getCandidateState().getCentralityState().duplicate();

		finallyCentralityState.setAllowsCentral(false);
		candidateCentralityState.setAllowsCentral(false);
		finallyCentralityState.setAllowsNonStartingNode(false);
		candidateCentralityState.setAllowsNonStartingNode(false);

		TraverserStateFactory<NoBlockTraverserState> finallyStateProducer =
				NoBlockTraverserState.getFactory(finallyCentralityState, finallyBlock);
		TraverserStateFactory<NoBlockTraverserState> candidateStateProducer =
				NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);

		return TraverserActivePathState.produceFromFactories(previousState, finallyStateProducer, candidateStateProducer);
	}

	private static TraverserActivePathState createStateForUnevenMatch(TraverserActivePathState previousState,
			TraverserState finallyState,
			TraverserState candidateState, BlockNode finallyBlock, BlockNode candidateBlock, int finallyInsnsSize,
			int candidateInsnsSize) {
		int maxIterateCount = Math.max(finallyInsnsSize, candidateInsnsSize);
		boolean finallyOverruns = finallyInsnsSize > candidateInsnsSize;

		int insnsDelta;
		TraverserStateFactory<?> newFinallyStateProducer;
		TraverserStateFactory<?> newCandidateStateProducer;
		TraverserBlockInfo adjustedBlockInfo;
		if (finallyOverruns) {
			// More finally instructions than candidate instructions
			CentralityState candidateCentralityState = candidateState.getCentralityState().duplicate();
			candidateCentralityState.setAllowsCentral(false);
			candidateCentralityState.setAllowsNonStartingNode(false);
			CentralityState finallyCentralityState = finallyState.getCentralityState();
			finallyCentralityState.setAllowsCentral(false);
			finallyCentralityState.setAllowsNonStartingNode(false);

			insnsDelta = finallyInsnsSize - maxIterateCount;
			newFinallyStateProducer = new DuplicatedTraverserStateFactory<>(finallyState);
			adjustedBlockInfo = finallyState.getBlockInsnInfo();
			newCandidateStateProducer = NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);
		} else {
			// More candidate instructions than finally instructions
			CentralityState finallyCentralityState = finallyState.getCentralityState().duplicate();
			finallyCentralityState.setAllowsCentral(false);
			finallyCentralityState.setAllowsNonStartingNode(false);
			CentralityState candidateCentralityState = candidateState.getCentralityState();
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

	private static TraverserActivePathState createStateForBlockSkip(TraverserActivePathState previousState,
			TraverserState finallyState,
			TraverserState candidateState, BlockNode finallyBlock, BlockNode candidateBlock) {
		CentralityState finallyCentralityState = finallyState.getCentralityState();
		CentralityState candidateCentralityState = candidateState.getCentralityState();

		// TODO: Maybe replace this with controller logic so that we can determine if we need to use these
		// as path ends and then merge above path?

		// Fix up finally path first. If this continues to fail, check if candidate can be fixed up in a
		// later iteration.
		if (finallyCentralityState.getAllowsNonStartingNode()) {
			finallyCentralityState.setAllowsNonStartingNode(false);
			TraverserStateFactory<NoBlockTraverserState> newFinallyStateProducer =
					NoBlockTraverserState.getFactory(finallyCentralityState, finallyBlock);
			TraverserStateFactory<?> newCandidateStateProducer = new DuplicatedTraverserStateFactory<>(candidateState);
			return TraverserActivePathState.produceFromFactories(previousState, newFinallyStateProducer, newCandidateStateProducer);
		} else {
			candidateCentralityState.setAllowsNonStartingNode(false);
			TraverserStateFactory<NoBlockTraverserState> newCandidateStateProducer =
					NoBlockTraverserState.getFactory(candidateCentralityState, candidateBlock);
			TraverserStateFactory<?> newFinallyStateProducer = new DuplicatedTraverserStateFactory<>(finallyState);
			return TraverserActivePathState.produceFromFactories(previousState, newFinallyStateProducer, newCandidateStateProducer);
		}
	}

	private static TraverserActivePathState createStateForTerminatorState(TraverserActivePathState previousState) {
		TraverserStateFactory<TerminalTraverserState> finallyStateProducer =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_INSTRUCTIONS);
		TraverserStateFactory<TerminalTraverserState> candidateStateProducer =
				TerminalTraverserState.getFactory(TerminalTraverserState.TerminationReason.NON_MATCHING_INSTRUCTIONS);

		return TraverserActivePathState.produceFromFactories(previousState, finallyStateProducer, candidateStateProducer);
	}

	private final SameInstructionsStrategy sameInstructionsStrategy = new SameInstructionsStrategyImpl();

	@Override
	public TraverserActivePathState visit(TraverserActivePathState state) {
		TraverserState finallyState = state.getFinallyState();
		TraverserState candidateState = state.getCandidateState();

		TraverserBlockInfo finallyBlockInfo = finallyState.getBlockInsnInfo();
		TraverserBlockInfo candidateBlockInfo = candidateState.getBlockInsnInfo();

		if (finallyBlockInfo == null || candidateBlockInfo == null) {
			throw new UnsupportedOperationException(
					"The instruction comparator handler has received a state which does not support block insn info");
		}

		BlockNode finallyBlock = finallyBlockInfo.getBlock();
		BlockNode candidateBlock = candidateBlockInfo.getBlock();

		List<InsnNode> finallyInsns = finallyBlockInfo.getInsnsSlice();
		List<InsnNode> candidateInsns = candidateBlockInfo.getInsnsSlice();
		int finallyInsnsSize = finallyInsns.size();
		int candidateInsnsSize = candidateInsns.size();

		int maxIterateCount = Math.min(finallyInsnsSize, candidateInsnsSize);

		List<Pair<InsnNode>> matchingInsns = new ArrayList<>(maxIterateCount);

		// Search through each instruction in reverse and see how many match
		for (int i = 0; i < maxIterateCount; i++) {
			InsnNode candidateInsn = candidateInsns.get(candidateInsnsSize - i - 1);
			InsnNode finallyInsn = finallyInsns.get(finallyInsnsSize - i - 1);

			if (!sameInstructionsStrategy.sameInsns(candidateInsn, finallyInsn)) {
				break;
			}

			Pair<InsnNode> match = new Pair<>(finallyInsn, candidateInsn);
			matchingInsns.add(match);
		}

		int matchedInsnsCount = matchingInsns.size();

		state.registerWithBlockInfo(finallyBlockInfo, matchedInsnsCount);
		state.registerWithBlockInfo(candidateBlockInfo, matchedInsnsCount);

		boolean finallyOverruns = finallyInsnsSize > candidateInsnsSize;
		boolean candidateOverruns = finallyInsnsSize < candidateInsnsSize;
		boolean sameSizedSlices = !finallyOverruns && !candidateOverruns;
		boolean allMatched = matchedInsnsCount == maxIterateCount;
		boolean noneMatched = matchedInsnsCount == 0;

		state.getMatchedInsns().addAll(matchingInsns);

		TraverserActivePathState newState;
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

	private boolean eitherStateAllowsBlockSkip(TraverserState finallyState, TraverserState candidateState) {
		CentralityState finallyCentralityState = finallyState.getCentralityState();
		CentralityState candidateCentralityState = candidateState.getCentralityState();

		return finallyCentralityState.getAllowsNonStartingNode() || candidateCentralityState.getAllowsNonStartingNode();
	}
}
