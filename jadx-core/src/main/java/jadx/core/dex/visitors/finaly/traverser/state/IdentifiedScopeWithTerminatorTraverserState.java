package jadx.core.dex.visitors.finaly.traverser.state;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.factory.TraverserStateFactory;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;
import jadx.core.dex.visitors.finaly.traverser.handlers.MergePathActivePathTraverserHandler;

public final class IdentifiedScopeWithTerminatorTraverserState extends TraverserState {

	public static TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> getFactory(final CentralityState centralityState,
			final List<BlockNode> roots, final BlockNode scopeTerminator) {
		return new IdentifiedScopeWithTerminatorStateFactory(centralityState, roots, scopeTerminator);
	}

	private static final class IdentifiedScopeWithTerminatorStateFactory
			extends TraverserStateFactory<IdentifiedScopeWithTerminatorTraverserState> {

		private final CentralityState centralityState;
		private final List<BlockNode> roots;
		private final BlockNode scopeTerminator;

		public IdentifiedScopeWithTerminatorStateFactory(final CentralityState centralityState, final List<BlockNode> roots,
				final BlockNode scopeTerminator) {
			this.centralityState = centralityState;
			this.roots = roots;
			this.scopeTerminator = scopeTerminator;
		}

		@Override
		public final IdentifiedScopeWithTerminatorTraverserState generateInternalState(final TraverserActivePathState state) {
			return new IdentifiedScopeWithTerminatorTraverserState(state, centralityState, roots, scopeTerminator);
		}
	}

	private final CentralityState centralityState;
	private final List<BlockNode> roots;
	private final BlockNode scopeTerminator;

	public IdentifiedScopeWithTerminatorTraverserState(final TraverserActivePathState state, final CentralityState centralityState,
			final List<BlockNode> roots, final BlockNode scopeTerminator) {
		super(state);
		this.roots = roots;
		this.scopeTerminator = scopeTerminator;
		this.centralityState = centralityState;
	}

	@Override
	public final @Nullable AbstractBlockTraverserHandler getNextHandler() {
		return new MergePathActivePathTraverserHandler(getComparatorState());
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
		return new IdentifiedScopeWithTerminatorTraverserState(comparatorState, centralityState, roots, scopeTerminator);
	}

	public final BlockNode getTerminus() {
		return scopeTerminator;
	}

	public final List<BlockNode> getRoots() {
		return roots;
	}
}
