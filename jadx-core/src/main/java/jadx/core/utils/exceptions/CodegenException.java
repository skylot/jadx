package jadx.core.utils.exceptions;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.MethodNode;

public class CodegenException extends JadxException {

	private static final long serialVersionUID = 39344288912966824L;

	public CodegenException(String message) {
		super(message);
	}

	public CodegenException(String message, Throwable cause) {
		super(message, cause);
	}

	public CodegenException(ClassNode mth, String msg) {
		super(mth, msg, null);
	}

	public CodegenException(ClassNode mth, String msg, Throwable th) {
		super(mth, msg, th);
	}

	public CodegenException(MethodNode mth, String msg) {
		super(mth, msg, null);
	}

	public CodegenException(MethodNode mth, String msg, Throwable th) {
		super(mth, msg, th);
	}

}
