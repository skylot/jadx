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
		public UnresolvableBlockException(BlockNode block, String reason) {
			super("A block, " + block.toString() + ", could not have instructions compared.\n\t" + reason);
		}
	}

	public InstructionActivePathTraverserHandler(TraverserActivePathState state) {
		super(state);
	}

	@Override
	protected List<TraverserActivePathState> handle() throws TraverserException {
		TraverserActivePathState comparator = getComparator();
		TraverserGlobalCommonState commonState = comparator.getGlobalCommonState();

		TraverserState finallyState = comparator.getFinallyState();
		TraverserState candidateState = comparator.getCandidateState();

		TraverserBlockInfo finallyBlockInfo = finallyState.getBlockInsnInfo();
		TraverserBlockInfo candidateBlockInfo = candidateState.getBlockInsnInfo();
		BlockNode finallyBlock = finallyBlockInfo.getBlock();
		BlockNode candidateBlock = candidateBlockInfo.getBlock();

		InstructionBlockComparatorTraverserVisitor visitor = new InstructionBlockComparatorTraverserVisitor();
		TraverserActivePathState newState = visitor.visit(comparator);

		if (finallyBlock != null && candidateBlock != null) {
			commonState.addCachedStateFor(finallyBlock, candidateBlock, List.of(newState));
		}
		return List.of(newState);
	}
}
