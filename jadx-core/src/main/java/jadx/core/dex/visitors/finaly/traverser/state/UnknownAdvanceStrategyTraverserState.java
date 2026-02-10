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

	public UnknownAdvanceStrategyTraverserState(final TraverserActivePathState state, final CentralityState centralityState,
			final List<BlockNode> nextBlocks) {
		super(state);

		this.centralityState = centralityState;
		this.nextBlocks = nextBlocks;
	}

	@Override
	public final @Nullable AbstractActivePathTraverserHandler getNextHandler() {
		return new PredecessorMergeActivePathTraverserHandler(getComparatorState());
	}

	@Override
	public final ComparisonState getCompareState() {
		return ComparisonState.READY_TO_COMPARE;
	}

	@Override
	public final boolean isTerminal() {
		return false;
	}

	@Override
	protected final @Nullable CentralityState getUnderlyingCentralityState() {
		return centralityState;
	}

	@Override
	protected final @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo() {
		return null;
	}

	@Override
	protected final TraverserState duplicateInternalState(final TraverserActivePathState comparatorState) {
		final CentralityState dCentralityState = centralityState.duplicate();
		final List<BlockNode> dNextBlocks = new ArrayList<>(nextBlocks);

		final TraverserState duplicated = new UnknownAdvanceStrategyTraverserState(comparatorState, dCentralityState, dNextBlocks);

		return duplicated;
	}

	public final List<BlockNode> getNextBlocks() {
		return nextBlocks;
	}
}
