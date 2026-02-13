package jadx.core.dex.visitors.finaly.traverser.visitors.comparator;

import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;

public abstract class AbstractTraverserComparatorVisitor {

	public abstract TraverserActivePathState visit(final TraverserActivePathState state);
}
