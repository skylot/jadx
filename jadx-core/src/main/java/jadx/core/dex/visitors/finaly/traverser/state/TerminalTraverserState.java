package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockPathTraverserHandler;

public final class TerminalTraverserState extends TraverserState {

	public static TraverserStateFactory<TerminalTraverserState> getFactory(final TerminationReason terminationReason) {
		return new TerminalStateFactory(terminationReason);
	}

	public static enum TerminationReason {
		/**
		 * When comparing instructions within a finally and candidate block, non-matching
		 * instructions were found calling for the termination of the Traverser.
		 */
		NON_MATCHING_INSTRUCTIONS,

		NON_MATCHING_PATHS,

		/**
		 * When a handler was requested to find the predecessors of a block, no predecessors within
		 * the scope existed.
		 */
		END_OF_PATH,
		/**
		 * When a handler was requested to process a block, a cached result for that handler
		 * already existed.
		 */
		USING_CACHED_RESULTS,

		UNMERGEABLE_STATE,

		UNRESOLVABLE_STATES,
	}

	private static class TerminalStateFactory extends TraverserStateFactory<TerminalTraverserState> {

		private final TerminationReason terminationReason;

		public TerminalStateFactory(final TerminationReason terminationReason) {
			this.terminationReason = terminationReason;
		}

		@Override
		public TerminalTraverserState generateInternalState(TraverserActivePathState state) {
			return new TerminalTraverserState(state, terminationReason);
		}
	}

	private final TerminationReason terminationReason;

	public TerminalTraverserState(final TraverserActivePathState state, final TerminationReason terminationReason) {
		super(state);
		this.terminationReason = terminationReason;
	}

	@Override
	public final boolean isTerminal() {
		return true;
	}

	@Override
	@Nullable
	public final AbstractBlockPathTraverserHandler getNextHandler() {
		return null;
	}

	public final TerminationReason getTerminationReason() {
		return terminationReason;
	}

	@Override
	public final ComparisonState getCompareState() {
		return ComparisonState.NOT_READY;
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
		final TraverserState duplicated = new TerminalTraverserState(comparatorState, terminationReason);

		return duplicated;
	}
}
