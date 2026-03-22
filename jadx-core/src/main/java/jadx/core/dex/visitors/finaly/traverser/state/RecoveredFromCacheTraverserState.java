package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;

public final class RecoveredFromCacheTraverserState extends TraverserState {

	public static TraverserStateFactory<RecoveredFromCacheTraverserState> getFactory(TraverserState underlying) {
		return new RecoveredFromCacheStateFactory(underlying);
	}

	private static final class RecoveredFromCacheStateFactory extends TraverserStateFactory<RecoveredFromCacheTraverserState> {
		private final TraverserState underlying;

		private RecoveredFromCacheStateFactory(TraverserState underlying) {
			this.underlying = underlying;
		}

		@Override
		protected RecoveredFromCacheTraverserState generateInternalState(TraverserActivePathState state) {
			return new RecoveredFromCacheTraverserState(underlying);
		}

	}

	private final TraverserState underlying;

	public RecoveredFromCacheTraverserState(TraverserState underlying) {
		super(underlying.getComparatorState());
		this.underlying = underlying;
	}

	@Override
	public @Nullable AbstractBlockTraverserHandler getNextHandler() {
		return null;
	}

	@Override
	public ComparisonState getCompareState() {
		return ComparisonState.NOT_READY;
	}

	@Override
	public boolean isTerminal() {
		return true;
	}

	@Override
	protected @Nullable CentralityState getUnderlyingCentralityState() {
		return null;
	}

	@Override
	protected @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo() {
		return null;
	}

	@Override
	protected TraverserState duplicateInternalState(TraverserActivePathState comparatorState) {
		return new RecoveredFromCacheTraverserState(underlying);
	}

	public TraverserState getUnderlying() {
		return underlying;
	}

	public boolean canContinue() {
		return underlying.isTerminal();
	}
}
