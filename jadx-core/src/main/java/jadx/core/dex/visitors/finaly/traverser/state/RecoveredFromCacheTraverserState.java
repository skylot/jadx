package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;

public final class RecoveredFromCacheTraverserState extends TraverserState {

	public static TraverserStateFactory<RecoveredFromCacheTraverserState> getFactory(final TraverserState underlying) {
		return new RecoveredFromCacheStateFactory(underlying);
	}

	private static final class RecoveredFromCacheStateFactory extends TraverserStateFactory<RecoveredFromCacheTraverserState> {

		private final TraverserState underlying;

		private RecoveredFromCacheStateFactory(final TraverserState underlying) {
			this.underlying = underlying;
		}

		@Override
		protected final RecoveredFromCacheTraverserState generateInternalState(final TraverserActivePathState state) {
			return new RecoveredFromCacheTraverserState(underlying);
		}

	}

	private final TraverserState underlying;

	public RecoveredFromCacheTraverserState(final TraverserState underlying) {
		super(underlying.getComparatorState());

		this.underlying = underlying;
	}

	@Override
	public final @Nullable AbstractBlockTraverserHandler getNextHandler() {
		return null;
	}

	@Override
	public final ComparisonState getCompareState() {
		return ComparisonState.NOT_READY;
	}

	@Override
	public final boolean isTerminal() {
		return true;
	}

	@Override
	protected final @Nullable CentralityState getUnderlyingCentralityState() {
		return null;
	}

	@Override
	protected final @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo() {
		return null;
	}

	@Override
	protected final TraverserState duplicateInternalState(final TraverserActivePathState comparatorState) {
		return new RecoveredFromCacheTraverserState(underlying);
	}

	public final TraverserState getUnderlying() {
		return underlying;
	}

	public final boolean canContinue() {
		return underlying.isTerminal();
	}
}
