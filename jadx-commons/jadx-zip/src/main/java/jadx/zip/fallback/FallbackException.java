package jadx.zip.fallback;

import java.io.IOException;

public class FallbackException extends IOException {
	public FallbackException(String message, Throwable cause) {
		super(message, cause);
	}
}
