package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockPathTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.PredecessorBlockPathTraverserHandler;

public final class NoBlockTraverserState extends TraverserState implements ISourceBlockState {

	public static TraverserStateFactory<NoBlockTraverserState> getFactory(final CentralityState centralityState,
			final BlockNode sourceBlock) {
		return new NoBlockStateFactory(centralityState, sourceBlock);
	}

	private static class NoBlockStateFactory extends TraverserStateFactory<NoBlockTraverserState> {

		private final CentralityState centralityState;
		private final BlockNode sourceBlock;

		public NoBlockStateFactory(final CentralityState centralityState, final BlockNode sourceBlock) {
			this.centralityState = centralityState;
			this.sourceBlock = sourceBlock;
		}

		@Override
		public NoBlockTraverserState generateInternalState(final TraverserActivePathState state) {
			return new NoBlockTraverserState(state, centralityState, sourceBlock);
		}
	}

	private final BlockNode sourceBlock;
	private final CentralityState centralityState;

	public NoBlockTraverserState(final TraverserActivePathState state, final CentralityState centralityState, final BlockNode sourceBlock) {
		super(state);
		this.sourceBlock = sourceBlock;
		this.centralityState = centralityState;
	}

	@Override
	public final @Nullable AbstractBlockPathTraverserHandler getNextHandler() {
		return new PredecessorBlockPathTraverserHandler<>(this);
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
	protected final @Nullable CentralityState getUnderlyingCentralityState() {
		return centralityState;
	}

	@Override
	protected final @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo() {
		return null;
	}

	@Override
	public final BlockNode getSourceBlock() {
		return sourceBlock;
	}

	@Override
	protected final TraverserState duplicateInternalState(final TraverserActivePathState comparatorState) {
		final CentralityState dCentralityState = centralityState.duplicate();
		return new NoBlockTraverserState(comparatorState, dCentralityState, sourceBlock);
	}
}
