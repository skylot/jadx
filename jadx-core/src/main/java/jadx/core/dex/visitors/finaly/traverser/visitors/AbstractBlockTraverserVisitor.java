package jadx.core.dex.visitors.finaly.traverser.visitors;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;

public abstract class AbstractBlockTraverserVisitor {

	private final TraverserState state;

	public AbstractBlockTraverserVisitor(TraverserState state) {
		this.state = state;
	}

	public abstract TraverserState visit(BlockNode block);

	public TraverserState getState() {
		return state;
	}

	public TraverserActivePathState getComparator() {
		return state.getComparatorState();
	}
}
