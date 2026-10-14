package jadx.mcp.jadx;

import jadx.mcp.tools.CancellationToken;

/**
 * Char sequence for regex matching with cancellation checks.
 * Protects worker threads from catastrophic backtracking in user provided patterns.
 */
final class CancellableCharSequence implements CharSequence {
	private static final int CHECK_INTERVAL = 0xFFF;

	private final CharSequence inner;
	private final CancellationToken token;
	private int counter;

	CancellableCharSequence(CharSequence inner, CancellationToken token) {
		this.inner = inner;
		this.token = token;
	}

	@Override
	public char charAt(int index) {
		if ((++counter & CHECK_INTERVAL) == 0) {
			token.throwIfCancelled();
		}
		return inner.charAt(index);
	}

	@Override
	public int length() {
		return inner.length();
	}

	@Override
	public CharSequence subSequence(int start, int end) {
		return new CancellableCharSequence(inner.subSequence(start, end), token);
	}

	@Override
	public String toString() {
		return inner.toString();
	}
}
