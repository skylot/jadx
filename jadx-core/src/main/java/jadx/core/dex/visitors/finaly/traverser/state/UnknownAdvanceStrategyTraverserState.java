package jadx.core.dex.visitors.finaly.traverser.state;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractActivePathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.PredecessorMergeActivePathTraverserHandler;

public final class UnknownAdvanceStrategyTraverserState extends TraverserState {
	private final CentralityState centralityState;
	private final List<BlockNode> nextBlocks;

	public UnknownAdvanceStrategyTraverserState(TraverserActivePathState state, CentralityState centralityState,
			List<BlockNode> nextBlocks) {
		super(state);
		this.centralityState = centralityState;
		this.nextBlocks = nextBlocks;
	}

	@Override
	public @Nullable AbstractActivePathTraverserHandler getNextHandler() {
		return new PredecessorMergeActivePathTraverserHandler(getComparatorState());
	}

	@Override
	public ComparisonState getCompareState() {
		return ComparisonState.READY_TO_COMPARE;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	protected @Nullable CentralityState getUnderlyingCentralityState() {
		return centralityState;
	}

	@Override
	protected @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo() {
		return null;
	}

	@Override
	protected TraverserState duplicateInternalState(TraverserActivePathState comparatorState) {
		CentralityState dCentralityState = centralityState.duplicate();
		List<BlockNode> dNextBlocks = new ArrayList<>(nextBlocks);
		return new UnknownAdvanceStrategyTraverserState(comparatorState, dCentralityState, dNextBlocks);
	}

	public List<BlockNode> getNextBlocks() {
		return nextBlocks;
	}
}
