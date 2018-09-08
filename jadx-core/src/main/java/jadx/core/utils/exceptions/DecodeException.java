package jadx.core.utils.exceptions;

import jadx.core.dex.nodes.MethodNode;

public class DecodeException extends JadxException {

	private static final long serialVersionUID = -6611189094923499636L;

	public DecodeException(String message) {
		super(message);
	}

	public DecodeException(String message, Throwable cause) {
		super(message, cause);
	}

	public DecodeException(MethodNode mth, String msg) {
		super(mth, msg, null);
	}

	public DecodeException(MethodNode mth, String msg, Throwable th) {
		super(mth, msg, th);
	}
}
