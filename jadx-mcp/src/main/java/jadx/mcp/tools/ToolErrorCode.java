package jadx.mcp.tools;

/**
 * Error codes returned in tool execution errors ('isError' results)
 */
public enum ToolErrorCode {
	INVALID_ARGUMENT,
	NOT_FOUND,
	AMBIGUOUS,
	PROJECT_NOT_LOADED,
	PROJECT_LOADING,
	PROJECT_CLOSING,
	PROJECT_CHANGED,
	INVALID_CURSOR,
	CANCELLED,
	TIMEOUT,
	BUSY,
	UNSUPPORTED,
	WRITE_DISABLED,
	OPERATION_FAILED,
	INTERNAL_ERROR
}
