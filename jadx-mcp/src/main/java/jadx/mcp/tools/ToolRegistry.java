package jadx.mcp.tools;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jadx.mcp.protocol.JsonRpc;

/**
 * Tools collection: listing, argument validation and result/error formatting
 */
public final class ToolRegistry {
	private static final Logger LOG = LoggerFactory.getLogger(ToolRegistry.class);

	private final Map<String, McpTool> tools = new LinkedHashMap<>();
	private final BooleanSupplier writeEnabled;
	private boolean validateOutput;

	public ToolRegistry(BooleanSupplier writeEnabled) {
		this.writeEnabled = writeEnabled;
	}

	public void register(McpTool tool) {
		if (tools.putIfAbsent(tool.getName(), tool) != null) {
			throw new IllegalArgumentException("Duplicate tool name: " + tool.getName());
		}
	}

	/**
	 * Check structured results against declared output schema (for tests)
	 */
	public void setValidateOutput(boolean validateOutput) {
		this.validateOutput = validateOutput;
	}

	public @Nullable McpTool get(String name) {
		return tools.get(name);
	}

	public Collection<McpTool> getAll() {
		return Collections.unmodifiableCollection(tools.values());
	}

	public boolean isWriteEnabled() {
		return writeEnabled.getAsBoolean();
	}

	/**
	 * Tools visible to clients. Modifying tools are hidden if writes disabled in settings.
	 */
	public List<McpTool> listAvailable() {
		boolean writes = isWriteEnabled();
		List<McpTool> list = new ArrayList<>(tools.size());
		for (McpTool tool : tools.values()) {
			if (tool.isReadOnly() || writes) {
				list.add(tool);
			}
		}
		return list;
	}

	public JsonArray listJson(boolean includeOutputSchema) {
		JsonArray arr = new JsonArray();
		for (McpTool tool : listAvailable()) {
			arr.add(tool.toJson(includeOutputSchema));
		}
		return arr;
	}

	/**
	 * Execute tool. Tool level errors returned as 'isError' result.
	 *
	 * @return CallToolResult object
	 */
	public JsonObject call(McpTool tool, @Nullable JsonObject arguments, ToolCallContext ctx, boolean structuredOutput) {
		try {
			if (!tool.isReadOnly() && !isWriteEnabled()) {
				throw new ToolException(ToolErrorCode.WRITE_DISABLED,
						"Modifying tools are disabled in jadx MCP settings (Preferences > MCP Server)");
			}
			JsonObject args = arguments == null ? new JsonObject() : arguments;
			List<String> errors = SchemaValidator.validate(tool.getInputSchema(), args);
			if (!errors.isEmpty()) {
				ToolException ex = ToolException.invalidArg("Invalid arguments: " + String.join("; ", errors));
				ex.detail("inputSchema", tool.getInputSchema());
				throw ex;
			}
			ctx.checkCancelled();
			JsonObject result = tool.getHandler().call(ctx, new ToolArgs(args, tool.getInputSchema()));
			ctx.checkCancelled();
			if (validateOutput && tool.getOutputSchema() != null) {
				List<String> outErrors = SchemaValidator.validate(tool.getOutputSchema(), result);
				if (!outErrors.isEmpty()) {
					throw new IllegalStateException("Tool '" + tool.getName() + "' output doesn't match schema: " + outErrors);
				}
			}
			return success(result, structuredOutput);
		} catch (ToolException e) {
			return error(e.getCode(), e.getMessage(), e.getDetails());
		} catch (ToolCancelledException e) {
			return cancelled(e.getReason());
		} catch (IllegalStateException e) {
			if (validateOutput) {
				throw e;
			}
			LOG.warn("MCP tool '{}' failed", tool.getName(), e);
			return error(ToolErrorCode.INTERNAL_ERROR, describeException(e), null);
		} catch (Exception | StackOverflowError e) {
			LOG.warn("MCP tool '{}' failed", tool.getName(), e);
			return error(ToolErrorCode.INTERNAL_ERROR, describeException(e), null);
		} catch (OutOfMemoryError e) {
			LOG.error("MCP tool '{}' failed: out of memory", tool.getName());
			return error(ToolErrorCode.INTERNAL_ERROR,
					"Out of memory. Reduce result size (use paging/limits) or increase jadx heap size.", null);
		}
	}

	public static JsonObject success(JsonObject structured, boolean structuredOutput) {
		JsonObject result = new JsonObject();
		result.add("content", textContent(JsonRpc.GSON.toJson(structured)));
		if (structuredOutput) {
			result.add("structuredContent", structured);
		}
		result.addProperty("isError", false);
		return result;
	}

	public static JsonObject error(ToolErrorCode code, String message, @Nullable JsonObject details) {
		JsonObject err = new JsonObject();
		err.addProperty("code", code.name());
		err.addProperty("message", message);
		if (details != null) {
			for (Map.Entry<String, JsonElement> entry : details.entrySet()) {
				err.add(entry.getKey(), entry.getValue());
			}
		}
		JsonObject wrap = new JsonObject();
		wrap.add("error", err);
		JsonObject result = new JsonObject();
		result.add("content", textContent(JsonRpc.GSON.toJson(wrap)));
		result.addProperty("isError", true);
		return result;
	}

	public static JsonObject cancelled(CancellationToken.Reason reason) {
		switch (reason) {
			case TIMEOUT:
				return error(ToolErrorCode.TIMEOUT,
						"Tool call timed out. Narrow the request (filters, smaller limit/page) or continue with returned cursor.",
						null);
			case PROJECT_CLOSED:
				return error(ToolErrorCode.PROJECT_CLOSING,
						"Project was closed or reloaded during the call. Retry after project is loaded.", null);
			default:
				return error(ToolErrorCode.CANCELLED, "Tool call cancelled: " + reason, null);
		}
	}

	private static JsonArray textContent(String text) {
		JsonObject textContent = new JsonObject();
		textContent.addProperty("type", "text");
		textContent.addProperty("text", text);
		JsonArray content = new JsonArray();
		content.add(textContent);
		return content;
	}

	private static String describeException(Throwable e) {
		String msg = e.getMessage();
		return e.getClass().getSimpleName() + (msg == null ? "" : ": " + msg);
	}
}
