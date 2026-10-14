package jadx.mcp.tools;

/**
 * Thrown from cancellation checkpoints
 */
public class ToolCancelledException extends RuntimeException {
	private static final long serialVersionUID = 6007215312658454718L;

	private final CancellationToken.Reason reason;

	public ToolCancelledException(CancellationToken.Reason reason) {
		super("Operation cancelled: " + reason);
		this.reason = reason;
	}

	public CancellationToken.Reason getReason() {
		return reason;
	}
}
