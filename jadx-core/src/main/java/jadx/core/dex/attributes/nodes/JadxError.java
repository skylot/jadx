package jadx.core.dex.attributes.nodes;

import jadx.core.utils.Utils;

public class JadxError {

	private final String error;
	private final Throwable cause;

	public JadxError(Throwable cause) {
		this(null, cause);
	}

	public JadxError(String error, Throwable cause) {
		this.error = error;
		this.cause = cause;
	}

	public String getError() {
		return error;
	}

	public Throwable getCause() {
		return cause;
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder();
		str.append("JadxError: ");
		if (error != null) {
			str.append(error);
			str.append(' ');
		}
		if (cause != null) {
			str.append(cause.getClass());
			str.append(":");
			str.append(cause.getMessage());
			str.append("\n");
			str.append(Utils.getStackTrace(cause));
		}
		return str.toString();
	}
}
