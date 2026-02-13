package jadx.core.dex.visitors.finaly.traverser.handlers;

import java.util.concurrent.atomic.AtomicReference;

import jadx.core.dex.visitors.finaly.traverser.TraverserException;
import jadx.core.dex.visitors.finaly.traverser.state.TraverserState;

/**
 * Traverser handlers are responsible for deducing how blocks should be searched within a path
 * whilst
 * searching for duplicate 'finally' instructions.
 */
public abstract class AbstractBlockPathTraverserHandler extends AbstractBlockTraverserHandler {

	private final AtomicReference<? extends TraverserState> stateRef;

	public AbstractBlockPathTraverserHandler(final TraverserState initialState) {
		this.stateRef = new AtomicReference<>(initialState);
	}

	public AbstractBlockPathTraverserHandler(final AtomicReference<? extends TraverserState> initialStateRef) {
		this.stateRef = initialStateRef;
	}

	protected abstract void handle() throws TraverserException;

	public final void process() throws TraverserException {
		handle();
	}

	public final TraverserState getState() {
		return stateRef.get();
	}

	public final AtomicReference<? extends TraverserState> getStateReference() {
		return stateRef;
	}
}
