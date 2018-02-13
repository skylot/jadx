package jadx.core.utils.exceptions;

public class JadxArgsValidateException extends RuntimeException {

	private static final long serialVersionUID = -7457621776087311909L;

	public JadxArgsValidateException(String message) {
		super(message);
	}

	public JadxArgsValidateException(String message, Throwable cause) {
		super(message, cause);
	}
}
