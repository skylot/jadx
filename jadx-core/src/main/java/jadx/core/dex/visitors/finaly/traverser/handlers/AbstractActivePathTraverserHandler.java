package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.List;

import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserActivePathState;

public abstract class AbstractActivePathTraverserHandler extends AbstractBlockTraverserHandler {

	private final TraverserActivePathState comparatorState;

	public AbstractActivePathTraverserHandler(final TraverserActivePathState comparatorState) {
		this.comparatorState = comparatorState;
	}

	protected abstract List<TraverserActivePathState> handle() throws TraverserException;

	public final List<TraverserActivePathState> process() throws TraverserException {
		return handle();
	}

	public final TraverserActivePathState getComparator() {
		return comparatorState;
	}
}
