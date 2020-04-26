package jadx.core.utils.exceptions;

import java.util.Arrays;

import jadx.core.utils.Utils;

import static jadx.core.codegen.CodeWriter.NL;

public class JadxRuntimeException extends RuntimeException {

	private static final long serialVersionUID = -7410848445429898248L;

	public JadxRuntimeException(String message) {
		super(message);
	}

	public JadxRuntimeException(String... lines) {
		super(Utils.listToString(Arrays.asList(lines), NL + "  "));
	}

	public JadxRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}
}
