package jadx.core.dex.visitors.finaly.traverser.visitors;

import java.util.List;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.GlobalTraverserSourceState;
import jadx.core.dex.visitors.finaly.traverser.state.NewBlockTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TerminalTraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.dex.visitors.finaly.traverser.state.UnknownAdvanceStrategyTraverserState;
import jadx.core.utils.ListUtils;

public final class PredecessorBlockTraverserVisitor extends AbstractBlockTraverserVisitor {

	public PredecessorBlockTraverserVisitor(final TraverserState state) {
		super(state);
	}

	@Override
	public final TraverserState visit(final BlockNode block) {

		final TraverserState currentState = getState();
		final CentralityState centralityState = currentState.getCentralityState();
		final GlobalTraverserSourceState globalState = currentState.getGlobalState();

		final List<BlockNode> predecessors = block.getPredecessors();
		final List<BlockNode> containedPredecessors = ListUtils.filter(predecessors, globalState::isBlockContained);
		final int predecessorsCount = containedPredecessors.size();

		switch (predecessorsCount) {
			case 0:
				return new TerminalTraverserState(getComparator(), TerminalTraverserState.TerminationReason.END_OF_PATH);
			case 1:
				final BlockNode nextBlock = containedPredecessors.get(0);
				final TraverserBlockInfo blockInfo = new TraverserBlockInfo(nextBlock);
				return new NewBlockTraverserState(getComparator(), centralityState, blockInfo);
			default:
				return new UnknownAdvanceStrategyTraverserState(getComparator(), centralityState, containedPredecessors);
		}
	}

}
