package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockPathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.BaseBlockTraverserHandler;

public final class NewBlockTraverserState extends TraverserState {

	public static final TraverserStateFactory<NewBlockTraverserState> getFactory(final CentralityState centralityState,
			final TraverserBlockInfo blockInsnInfo) {
		return new NewBlockStateFactory(centralityState, blockInsnInfo);
	}

	private static class NewBlockStateFactory extends TraverserStateFactory<NewBlockTraverserState> {

		private final CentralityState centralityState;
		private final TraverserBlockInfo blockInsnInfo;

		public NewBlockStateFactory(final CentralityState centralityState, final TraverserBlockInfo blockInsnInfo) {
			this.centralityState = centralityState;
			this.blockInsnInfo = blockInsnInfo;
		}

		@Override
		public NewBlockTraverserState generateInternalState(final TraverserActivePathState state) {
			return new NewBlockTraverserState(state, centralityState, blockInsnInfo);
		}
	}

	private final CentralityState centralityState;
	private final @Nullable TraverserBlockInfo blockInsnInfo;

	public NewBlockTraverserState(final TraverserActivePathState state, final CentralityState centralityState,
			final TraverserBlockInfo blockInsnInfo) {
		super(state);
		this.centralityState = centralityState;
		this.blockInsnInfo = blockInsnInfo;
	}

	@Override
	public final ComparisonState getCompareState() {
		return ComparisonState.NOT_READY;
	}

	@Override
	public final boolean isTerminal() {
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
	protected final TraverserState duplicateInternalState(final TraverserActivePathState comparatorState) {
		final CentralityState dCentralityState = centralityState.duplicate();
		final TraverserBlockInfo dBlockInsnInfo = blockInsnInfo.duplicate();

		final TraverserState duplicated = new NewBlockTraverserState(comparatorState, dCentralityState, dBlockInsnInfo);

		return duplicated;
	}
}
