package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockPathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.BaseBlockTraverserHandler;

public final class NewBlockTraverserState extends TraverserState {

	public static TraverserStateFactory<NewBlockTraverserState> getFactory(CentralityState centralityState,
			TraverserBlockInfo blockInsnInfo) {
		return new NewBlockStateFactory(centralityState, blockInsnInfo);
	}

	private static class NewBlockStateFactory extends TraverserStateFactory<NewBlockTraverserState> {
		private final CentralityState centralityState;
		private final TraverserBlockInfo blockInsnInfo;

		public NewBlockStateFactory(CentralityState centralityState, TraverserBlockInfo blockInsnInfo) {
			this.centralityState = centralityState;
			this.blockInsnInfo = blockInsnInfo;
		}

		@Override
		public NewBlockTraverserState generateInternalState(TraverserActivePathState state) {
			return new NewBlockTraverserState(state, centralityState, blockInsnInfo);
		}
	}

	private final CentralityState centralityState;
	private final @Nullable TraverserBlockInfo blockInsnInfo;

	public NewBlockTraverserState(TraverserActivePathState state, CentralityState centralityState, TraverserBlockInfo blockInsnInfo) {
		super(state);
		this.centralityState = centralityState;
		this.blockInsnInfo = blockInsnInfo;
	}

	@Override
	public ComparisonState getCompareState() {
		return ComparisonState.NOT_READY;
	}

	@Override
	public boolean isTerminal() {
		return false;
	}

	@Override
	public @Nullable AbstractBlockPathTraverserHandler getNextHandler() {
		// We have no data on this block, so we'll give it the base block handler to gain
		// information about it to gather more state information regarding the block.
		return new BaseBlockTraverserHandler(this);
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
		return new NewBlockTraverserState(comparatorState, dCentralityState, dBlockInsnInfo);
	}
}
