package jadx.gui.device.debugger;

public class SmaliDebuggerException extends Exception {
	private final int errCode;
	private static final long serialVersionUID = -1111111202102191403L;

	public SmaliDebuggerException(Exception e) {
		super(e);
		errCode = -1;
	}

	public SmaliDebuggerException(String msg) {
		super(msg);
		this.errCode = -1;
	}

	public SmaliDebuggerException(String msg, Exception e) {
		super(msg, e);
		errCode = -1;
	}

	public SmaliDebuggerException(String msg, int errCode) {
		super(msg);
		this.errCode = errCode;
	}

	public int getErrCode() {
		return errCode;
	}
}
