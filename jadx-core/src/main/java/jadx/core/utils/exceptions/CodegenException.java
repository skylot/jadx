package jadx.core.utils.exceptions;

import jadx.core.dex.nodes.MethodNode;

public class CodegenException extends JadxException {

	private static final long serialVersionUID = 39344288912966824L;

	public CodegenException(String message) {
		super(message);
	}

	public CodegenException(MethodNode mth, String msg) {
		super(mth, msg, null);
	}

	public CodegenException(MethodNode mth, String msg, Throwable th) {
		super(mth, msg, th);
	}
}
