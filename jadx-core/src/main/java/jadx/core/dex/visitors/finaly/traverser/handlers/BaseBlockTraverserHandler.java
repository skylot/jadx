package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.concurrent.atomic.AtomicReference;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserBlockInfo;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;
import jadx.core.dex.visitors.finaly.traverser.visitors.ImplicitInsnBlockTraverserVisitor;
import jadx.core.dex.visitors.finaly.traverser.visitors.PathEndBlockTraverserVisitor;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class BaseBlockTraverserHandler extends AbstractBlockPathTraverserHandler {

	public BaseBlockTraverserHandler(final TraverserState initialState) {
		super(initialState);
	}

	public BaseBlockTraverserHandler(final AtomicReference<TraverserState> initialStateRef) {
		super(initialStateRef);
	}

	@Override
	protected void handle() {
		final TraverserBlockInfo blockInsnInfo = getState().getBlockInsnInfo();
		if (blockInsnInfo == null) {
			throw new JadxRuntimeException("Expected to find block info within " + getClass().getSimpleName());
		}

		final TraverserActivePathState comparator = getState().getComparatorState();
		final AtomicReference<TraverserState> stateRef = comparator.getReferenceForState(getState());

		if (stateRef == null) {
			throw new JadxRuntimeException("Orphaned traverser state");
		}

		final BlockNode block = blockInsnInfo.getBlock();

		final ImplicitInsnBlockTraverserVisitor implicitVisitor = new ImplicitInsnBlockTraverserVisitor(getState());
		final TraverserState stateAfterImplicit = implicitVisitor.visit(block);
		final PathEndBlockTraverserVisitor pathEndVisitor = new PathEndBlockTraverserVisitor(stateAfterImplicit);
		final TraverserState nextState = pathEndVisitor.visit(block);

		stateRef.set(nextState);
	}
}
