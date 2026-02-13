package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.InstructionActivePathTraverserHandler;

public final class AwaitingInsnCompareTraverserState extends TraverserState {

	private final CentralityState centralityState;
	private final @Nullable TraverserBlockInfo blockInsnInfo;

	public AwaitingInsnCompareTraverserState(final TraverserActivePathState state, final CentralityState centralityState,
			final TraverserBlockInfo blockInsnInfo) {
		super(state);

		this.centralityState = centralityState;
		this.blockInsnInfo = blockInsnInfo;
	}

	@Override
	public final @Nullable AbstractBlockTraverserHandler getNextHandler() {
		return new InstructionActivePathTraverserHandler(getComparatorState());
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
		return blockInsnInfo;
	}

	@Override
	protected final TraverserState duplicateInternalState(final TraverserActivePathState comparatorState) {
		final CentralityState dCentralityState = centralityState.duplicate();
		final TraverserBlockInfo dBlockInsnInfo = blockInsnInfo.duplicate();

		final TraverserState duplicated = new AwaitingInsnCompareTraverserState(comparatorState, dCentralityState, dBlockInsnInfo);

		return duplicated;
	}
}
