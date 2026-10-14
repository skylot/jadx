package jadx.mcp.jadx;

import java.util.function.BooleanSupplier;

import jadx.mcp.project.McpBackend;
import jadx.mcp.tools.ToolRegistry;

/**
 * Registry of all jadx MCP tools
 */
public final class JadxMcpTools {

	public static final String INSTRUCTIONS = "jadx decompiler for Android apps (APK/DEX/JAR/AAR). "
			+ "Tools work with the project currently opened in jadx-gui; call 'get_project_info' to check state. "
			+ "Nodes have stable ids based on original names: class 'com.a.Outer$Inner', "
			+ "method 'com.a.B->run(ILjava/lang/String;)V', field 'com.a.B->count:I'. "
			+ "Ids don't change after renames, while 'name' fields show current names. "
			+ "Tools also accept current/original full names and report AMBIGUOUS with candidate ids for overloads. "
			+ "Lists are paged with 'offset'/'limit' (see 'has_more', 'next_offset'); code is paged by lines "
			+ "('start_line', 'next_start_line'). Line numbers refer to decompiled code of the top-level class. "
			+ "Search tools return 'next_cursor' when stopped by time budget or results limit. "
			+ "Rename/comment tools change jadx project data (not the APK) and return after code is refreshed; "
			+ "'save.persisted' tells if changes are written to the .jadx project file.";

	private JadxMcpTools() {
	}

	public static ToolRegistry createRegistry(McpBackend backend, BooleanSupplier writeEnabled) {
		ToolRegistry registry = new ToolRegistry(writeEnabled);
		ProjectTools.register(registry, backend, writeEnabled);
		ClassTools.register(registry, backend);
		SearchTools.register(registry, backend);
		XrefTools.register(registry, backend);
		ResourceTools.register(registry, backend);
		EditTools.register(registry, backend);
		return registry;
	}
}
