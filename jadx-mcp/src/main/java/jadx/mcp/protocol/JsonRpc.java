package jadx.mcp.protocol;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * JSON-RPC 2.0 helpers
 */
public final class JsonRpc {
	public static final String VERSION = "2.0";

	public static final int PARSE_ERROR = -32700;
	public static final int INVALID_REQUEST = -32600;
	public static final int METHOD_NOT_FOUND = -32601;
	public static final int INVALID_PARAMS = -32602;
	public static final int INTERNAL_ERROR = -32603;

	/**
	 * MCP specific: request cancelled by client (used only if response can't be skipped)
	 */
	public static final int REQUEST_CANCELLED = -32800;
	/**
	 * Server specific: server overloaded or shutting down
	 */
	public static final int SERVER_BUSY = -32001;

	public static final Gson GSON = new GsonBuilder()
			.disableHtmlEscaping()
			.serializeNulls()
			.create();

	private JsonRpc() {
	}

	public static JsonObject result(JsonElement id, JsonElement result) {
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", VERSION);
		response.add("id", id);
		response.add("result", result);
		return response;
	}

	public static JsonObject error(@Nullable JsonElement id, int code, String message) {
		return error(id, code, message, null);
	}

	public static JsonObject error(@Nullable JsonElement id, int code, String message, @Nullable JsonElement data) {
		JsonObject error = new JsonObject();
		error.addProperty("code", code);
		error.addProperty("message", message);
		if (data != null) {
			error.add("data", data);
		}
		JsonObject response = new JsonObject();
		response.addProperty("jsonrpc", VERSION);
		response.add("id", id == null ? JsonNull.INSTANCE : id);
		response.add("error", error);
		return response;
	}

	public static JsonObject notification(String method, @Nullable JsonObject params) {
		JsonObject msg = new JsonObject();
		msg.addProperty("jsonrpc", VERSION);
		msg.addProperty("method", method);
		if (params != null) {
			msg.add("params", params);
		}
		return msg;
	}

	/**
	 * Valid request id: string or integer number (null is not allowed by MCP)
	 */
	public static boolean isValidId(@Nullable JsonElement id) {
		if (id == null || !id.isJsonPrimitive()) {
			return false;
		}
		JsonPrimitive primitive = id.getAsJsonPrimitive();
		if (primitive.isString()) {
			return true;
		}
		if (primitive.isNumber()) {
			double value = primitive.getAsDouble();
			return value == Math.rint(value) && !Double.isInfinite(value);
		}
		return false;
	}

	/**
	 * Unique key to compare request ids ('1' and 1 are different ids)
	 */
	public static String idKey(JsonElement id) {
		JsonPrimitive primitive = id.getAsJsonPrimitive();
		if (primitive.isString()) {
			return "s:" + primitive.getAsString();
		}
		return "n:" + primitive.getAsBigDecimal().stripTrailingZeros().toPlainString();
	}
}
