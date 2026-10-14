package jadx.mcp.tools;

import org.jetbrains.annotations.Nullable;

/**
 * Per call context: cancellation and client session info
 */
public final class ToolCallContext {
	private final CancellationToken token;
	private final @Nullable String sessionId;
	private final long deadlineNanos;

	public ToolCallContext(CancellationToken token, @Nullable String sessionId, long timeoutMs) {
		this.token = token;
		this.sessionId = sessionId;
		this.deadlineNanos = timeoutMs <= 0 ? Long.MAX_VALUE : System.nanoTime() + timeoutMs * 1_000_000L;
	}

	public CancellationToken getToken() {
		return token;
	}

	public @Nullable String getSessionId() {
		return sessionId;
	}

	public void checkCancelled() {
		token.throwIfCancelled();
	}

	/**
	 * Remaining time before call timeout
	 */
	public long remainingMs() {
		if (deadlineNanos == Long.MAX_VALUE) {
			return Long.MAX_VALUE;
		}
		return Math.max(0, (deadlineNanos - System.nanoTime()) / 1_000_000L);
	}
}
