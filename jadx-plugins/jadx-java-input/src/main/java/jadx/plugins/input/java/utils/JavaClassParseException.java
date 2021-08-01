package jadx.plugins.input.java.utils;

public class JavaClassParseException extends RuntimeException {
	private static final long serialVersionUID = -8452845601753645491L;

	public JavaClassParseException(String message, Throwable cause) {
		super(message, cause);
	}

	public JavaClassParseException(String message) {
		super(message);
	}
}
