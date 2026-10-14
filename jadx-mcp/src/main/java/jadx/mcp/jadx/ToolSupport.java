package jadx.mcp.jadx;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolArgs;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolException;

/**
 * Shared helpers and parameter schemas for jadx tools
 */
final class ToolSupport {
	static final int DEFAULT_LIMIT = 100;
	static final int MAX_LIMIT = 1000;

	private ToolSupport() {
	}

	@FunctionalInterface
	interface ProjectFunction {
		JsonObject apply(ProjectLease lease, NodeResolver resolver) throws ToolException;
	}

	/**
	 * Run function with project lease (released on exit)
	 */
	static JsonObject withProject(McpBackend backend, ToolCallContext ctx, ProjectFunction function) throws ToolException {
		try (ProjectLease lease = backend.getGate().acquire(ctx.getToken())) {
			return function.apply(lease, new NodeResolver(lease));
		}
	}

	static JsonObject classNameParam() {
		return Schema.nonEmptyString("Class id from other tools (e.g. 'com.example.Outer$Inner'), "
				+ "original or current full name ('com.example.Outer.Inner') or smali descriptor");
	}

	static JsonObject optClassNameParam() {
		return Schema.string("Declaring class (id or full name). Optional if member reference is a full id 'class->member'");
	}

	static JsonObject offsetParam() {
		return Schema.integer("Number of items to skip (paging)", 0, Integer.MAX_VALUE, 0L);
	}

	static JsonObject limitParam(int def, int max) {
		return Schema.integer("Max items to return", 1, max, (long) def);
	}

	static JsonObject startLineParam() {
		return Schema.integer("First line to return (1-based). Use 'next_start_line' from previous result to continue",
				1, Integer.MAX_VALUE, null);
	}

	static JsonObject maxLinesParam() {
		return Schema.integer("Max lines to return", 1, 1_000_000, null);
	}

	static JsonObject maxCharsParam() {
		return Schema.integer("Max characters of returned text (result is cut on line boundary)",
				1000, CodeText.MAX_CHARS_LIMIT, (long) CodeText.DEFAULT_MAX_CHARS);
	}

	static Schema.ObjectBuilder textPageParams(Schema.ObjectBuilder builder) {
		return builder
				.optional("start_line", startLineParam())
				.optional("max_lines", maxLinesParam())
				.optional("max_chars", maxCharsParam());
	}

	static JsonObject page(CodeText text, ToolArgs args) {
		return page(text, args, 1, Integer.MAX_VALUE);
	}

	/**
	 * Text page inside lines range, 'start_line' argument defaults to range start
	 */
	static JsonObject page(CodeText text, ToolArgs args, int rangeStart, int rangeEnd) {
		Integer startLine = args.getOptInt("start_line");
		int start = startLine == null ? rangeStart : Math.max(startLine, rangeStart);
		int maxChars = CodeText.normalizeMaxChars(args.getInt("max_chars", CodeText.DEFAULT_MAX_CHARS));
		return text.page(start, rangeEnd, args.getOptInt("max_lines"), maxChars);
	}

	static <T> JsonArray toArray(List<T> list, Function<T, JsonElement> mapper) {
		JsonArray arr = new JsonArray();
		for (T item : list) {
			arr.add(mapper.apply(item));
		}
		return arr;
	}

	/**
	 * Apply offset/limit and add paging info to result
	 */
	static <T> JsonArray pageList(JsonObject result, List<T> list, ToolArgs args, int defLimit,
			Function<T, JsonElement> mapper) {
		JsonArray arr = new JsonArray();
		for (T item : pageSubList(result, list, args, defLimit)) {
			arr.add(mapper.apply(item));
		}
		return arr;
	}

	/**
	 * Apply offset/limit, add paging info to result and return items of current page
	 */
	static <T> List<T> pageSubList(JsonObject result, List<T> list, ToolArgs args, int defLimit) {
		int offset = Math.max(0, args.getInt("offset", 0));
		int limit = Math.max(1, args.getInt("limit", defLimit));
		int total = list.size();
		int from = Math.min(offset, total);
		int to = (int) Math.min((long) from + limit, total);
		NodeJson.addPaging(result, total, from, to - from);
		return list.subList(from, to);
	}
}
