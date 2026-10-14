package jadx.mcp.tools;

import com.google.gson.JsonObject;

@FunctionalInterface
public interface ToolHandler {

	/**
	 * @return structured result object
	 */
	JsonObject call(ToolCallContext ctx, ToolArgs args) throws ToolException;
}
