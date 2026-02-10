package jadx.core.dex.trycatch;

import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import jadx.core.dex.nodes.BlockNode;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Represents an edge between two blocks representing an exit out of a try body.
 * The source block will be within the try body.
 */
public final class TryEdge {

	private final BlockNode source;
	private final BlockNode target;
	private final Optional<ExceptionHandler> handler;
	private final TryEdgeType type;

	public TryEdge(final BlockNode source, final BlockNode target, final TryEdgeType type) {
		this(source, target, type, Optional.empty());
	}

	public TryEdge(final BlockNode source, final BlockNode target, final @NotNull ExceptionHandler handler) {
		this(source, target, TryEdgeType.HANDLER, Optional.of(handler));
	}

	public TryEdge(final BlockNode source, final BlockNode target, final TryEdgeType type, final Optional<ExceptionHandler> handler) {
		this.source = source;
		this.target = target;
		this.handler = handler;
		this.type = type;

		if (isHandlerExit() && handler.isEmpty()) {
			throw new JadxRuntimeException("Attempted to add a null exception handler as an edge of \"" + type.toString() + "\" type");
		} else if (isNotHandlerExit() && handler.isPresent()) {
			throw new JadxRuntimeException("Attempted to add an exception handler as an edge of \"" + type.toString() + "\" type");
		}
	}

	@Override
	public final String toString() {
		StringBuilder sb = new StringBuilder("TryEdge: [");
		sb.append(type);
		sb.append(' ');
		sb.append(source.toString());
		sb.append(" -> ");
		sb.append(target.toString());
		sb.append("] - Handler: ");
		if (handler.isEmpty()) {
			sb.append("None");
		} else {
			sb.append(handler.get().toString());
		}
		return sb.toString();
	}

	@Override
	public final boolean equals(Object obj) {
		if (!(obj instanceof TryEdge)) {
			return false;
		}

		final TryEdge other = (TryEdge) obj;

		return source.equals(other.source)
				&& target.equals(other.target)
				&& handler.equals(other.handler)
				&& type.equals(other.type);
	}

	@Override
	public final int hashCode() {
		return Objects.hash(source, target, type, handler);
	}

	public final BlockNode getSource() {
		return source;
	}

	public final BlockNode getTarget() {
		return target;
	}

	public final TryEdgeType getType() {
		return type;
	}

	public final boolean isHandlerExit() {
		return type == TryEdgeType.HANDLER;
	}

	public final boolean isNotHandlerExit() {
		return !isHandlerExit();
	}

	public final ExceptionHandler getExceptionHandler() {
		if (!isHandlerExit()) {
			throw new JadxRuntimeException("Attempted to get the exception handler of a non-handler edge type");
		}

		if (handler.isEmpty()) {
			throw new JadxRuntimeException("Attempted to get the exception handler of a handler edge type, however none was present");
		}

		return handler.get();
	}
}
