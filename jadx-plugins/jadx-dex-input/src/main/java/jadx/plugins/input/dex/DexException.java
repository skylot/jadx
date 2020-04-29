package jadx.plugins.input.dex;

public class DexException extends RuntimeException {
	private static final long serialVersionUID = -5575702801815409269L;

	public DexException(String message, Throwable cause) {
		super(message, cause);
	}

	public DexException(String message) {
		super(message);
	}
}
