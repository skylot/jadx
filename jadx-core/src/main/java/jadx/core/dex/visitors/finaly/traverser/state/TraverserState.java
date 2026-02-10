package jadx.core.dex.visitors.finaly.traverser.state;

import org.jetbrains.annotations.Nullable;

import jadx.core.dex.visitors.finaly.CentralityState;
import jadx.core.dex.visitors.finaly.traverser.GlobalTraverserSourceState;
import jadx.core.dex.visitors.finaly.traverser.handlers.AbstractBlockTraverserHandler;

public abstract class TraverserState {

	public static enum ComparisonState {
		NOT_READY,
		AWAITING_OPTIONAL_PREDECESSOR_MERGE,
		READY_TO_COMPARE
	}

	private final TraverserActivePathState comparatorState;

	public TraverserState(final TraverserActivePathState comparatorState) {
		this.comparatorState = comparatorState;
	}

	@Nullable
	public abstract AbstractBlockTraverserHandler getNextHandler();

	public abstract ComparisonState getCompareState();

	public abstract boolean isTerminal();

	protected abstract @Nullable CentralityState getUnderlyingCentralityState();

	protected abstract @Nullable TraverserBlockInfo getUnderlyingBlockInsnInfo();

	/**
	 * Performs a deep clone of this Traverser state.
	 *
	 * @return The deep cloned duplication of this Traverser state.
	 */
	protected abstract TraverserState duplicateInternalState(final TraverserActivePathState comparatorState);

	@Override
	public final String toString() {
		return toString(0);
	}

	public final TraverserState duplicate(final TraverserActivePathState comparatorState) {
		final TraverserState duplicatedState = duplicateInternalState(comparatorState);
		return duplicatedState;
	}

	public final TraverserActivePathState getComparatorState() {
		return comparatorState;
	}

	public final String toString(final int indentAmount) {
		final String baseIndent = " ".repeat(indentAmount);
		final String secondIndent = " ".repeat(indentAmount + 2);

		final StringBuilder sb = new StringBuilder(baseIndent);
		sb.append(getClass().getSimpleName());
		sb.append(' ');

		if (isTerminal()) {
			sb.append("TERMINAL ");
		}

		sb.append(" {");
		sb.append(System.lineSeparator());

		sb.append(secondIndent);
		sb.append("centrality: ");
		final CentralityState centralityState = getUnderlyingCentralityState();
		if (centralityState == null) {
			sb.append("none");
		} else {
			sb.append(getCentralityState());
		}
		sb.append(System.lineSeparator());

		sb.append(secondIndent);
		sb.append(getCompareState());
		sb.append(System.lineSeparator());

		sb.append(secondIndent);
		final TraverserBlockInfo blockInsnInfo = getBlockInsnInfo();
		if (blockInsnInfo != null) {
			sb.append(blockInsnInfo.toString(secondIndent));
		} else {
			sb.append("NO ACTIVE BLOCK");
		}
		sb.append(System.lineSeparator());

		sb.append(baseIndent);
		sb.append("}");

		return sb.toString();
	}

	public final CentralityState getCentralityState() {
		final CentralityState underlying = getUnderlyingCentralityState();
		if (underlying == null) {
			throw new UnsupportedOperationException("Centrality state is not supported for " + getClass().getName());
		}
		return underlying;
	}

	public final @Nullable TraverserBlockInfo getBlockInsnInfo() {
		return getUnderlyingBlockInsnInfo();
	}

	public final GlobalTraverserSourceState getGlobalState() {
		return getComparatorState().getGlobalStateFor(this);
	}
}
