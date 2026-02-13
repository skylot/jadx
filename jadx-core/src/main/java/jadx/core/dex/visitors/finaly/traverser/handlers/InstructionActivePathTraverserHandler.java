package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserGlobalCommonState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.dex.visitors.finaly.traverser.visitors.comparator.InstructionBlockComparatorTraverserVisitor;

public final class InstructionActivePathTraverserHandler extends AbstractActivePathTraverserHandler {

	public static final class UnresolvableBlockException extends TraverserException {
		public UnresolvableBlockException(final BlockNode block, final String reason) {
			super("A block, " + block.toString() + ", could not have instructions compared.\n\t" + reason);
		}
	}

	public InstructionActivePathTraverserHandler(final TraverserActivePathState state) {
		super(state);
	}

	@Override
	protected List<TraverserActivePathState> handle() throws TraverserException {
		final TraverserActivePathState comparator = getComparator();
		final TraverserGlobalCommonState commonState = comparator.getGlobalCommonState();

		final TraverserState finallyState = comparator.getFinallyState();
		final TraverserState candidateState = comparator.getCandidateState();

		final TraverserBlockInfo finallyBlockInfo = finallyState.getBlockInsnInfo();
		final TraverserBlockInfo candidateBlockInfo = candidateState.getBlockInsnInfo();
		final BlockNode finallyBlock = finallyBlockInfo.getBlock();
		final BlockNode candidateBlock = candidateBlockInfo.getBlock();

		final InstructionBlockComparatorTraverserVisitor visitor = new InstructionBlockComparatorTraverserVisitor();
		final TraverserActivePathState newState = visitor.visit(comparator);

		if (finallyBlock != null && candidateBlock != null) {
			commonState.addCachedStateFor(finallyBlock, candidateBlock, List.of(newState));
		}

		return List.of(newState);
	}

}
