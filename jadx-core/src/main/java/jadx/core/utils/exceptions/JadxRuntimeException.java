package jadx.core.utils.exceptions;

public class JadxRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -7410848445429898248L;

	public JadxRuntimeException(String message) {
		super(message);
	}

	public JadxRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
