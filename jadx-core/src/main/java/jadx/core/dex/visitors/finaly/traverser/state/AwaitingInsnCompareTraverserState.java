package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.InstructionActivePathTraverserHandler;

public final class AwaitingInsnCompareTraverserState extends TraverserState {
	private final CentralityState centralityState;
	private final @Nullable TraverserBlockInfo blockInsnInfo;

	public AwaitingInsnCompareTraverserState(TraverserActivePathState state, CentralityState centralityState,
			TraverserBlockInfo blockInsnInfo) {
		super(state);
		this.centralityState = centralityState;
		this.blockInsnInfo = blockInsnInfo;
	}

	@Override
	public @Nullable AbstractBlockTraverserHandler getNextHandler() {
		return new InstructionActivePathTraverserHandler(getComparatorState());
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
		return blockInsnInfo;
	}

	@Override
	protected TraverserState duplicateInternalState(TraverserActivePathState comparatorState) {
		CentralityState dCentralityState = centralityState.duplicate();
		TraverserBlockInfo dBlockInsnInfo = blockInsnInfo.duplicate();
		return new AwaitingInsnCompareTraverserState(comparatorState, dCentralityState, dBlockInsnInfo);
	}
}
