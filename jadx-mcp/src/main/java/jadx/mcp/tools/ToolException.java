package jadx.mcp.tools;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Tool execution error. Reported to client as tool result with 'isError: true',
 * so a model can see the reason and adjust the call.
 */
public class ToolException extends Exception {
	private static final long serialVersionUID = -4573043466377018154L;

	private final ToolErrorCode code;
	private final JsonObject details = new JsonObject();

	public ToolException(ToolErrorCode code, String message) {
		super(message);
		this.code = code;
	}

	public ToolException(ToolErrorCode code, String message, @Nullable Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public ToolException detail(String key, @Nullable JsonElement value) {
		if (value != null) {
			details.add(key, value);
		}
		return this;
	}

	public ToolException detail(String key, @Nullable String value) {
		if (value != null) {
			details.addProperty(key, value);
		}
		return this;
	}

	public ToolException detail(String key, Number value) {
		details.addProperty(key, value);
		return this;
	}

	public ToolErrorCode getCode() {
		return code;
	}

	public JsonObject getDetails() {
		return details;
	}

	public static ToolException invalidArg(String message) {
		return new ToolException(ToolErrorCode.INVALID_ARGUMENT, message);
	}

	public static ToolException notFound(String message) {
		return new ToolException(ToolErrorCode.NOT_FOUND, message);
	}
}
