package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.concurrent.atomic.AtomicReference;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.traverser.state.ISourceBlockState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.dex.visitors.finaly.traverser.visitors.AbstractBlockTraverserVisitor;
import jadx.core.dex.visitors.finaly.traverser.visitors.PredecessorBlockTraverserVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class PredecessorBlockPathTraverserHandler<T extends TraverserState & ISourceBlockState>
		extends AbstractBlockPathTraverserHandler {
	private final ISourceBlockState sourceBlockState;

	public PredecessorBlockPathTraverserHandler(T initialState) {
		super(initialState);
		this.sourceBlockState = initialState;
	}

	public PredecessorBlockPathTraverserHandler(AtomicReference<T> initialStateRef) {
		super(initialStateRef);
		this.sourceBlockState = initialStateRef.get();
	}

	@Override
	protected void handle() {
		TraverserState baseState = getState();
		TraverserActivePathState comparator = baseState.getComparatorState();
		AtomicReference<TraverserState> stateRef = comparator.getReferenceForState(baseState);
		if (stateRef == null) {
			throw new JadxRuntimeException("Orphaned traverser state");
		}
		BlockNode sourceBlock = sourceBlockState.getSourceBlock();
		AbstractBlockTraverserVisitor visitor = new PredecessorBlockTraverserVisitor(baseState);
		TraverserState nextState = visitor.visit(sourceBlock);

		stateRef.set(nextState);
	}
}
