package jadx.mcp.jadx;

import java.util.function.BooleanSupplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.mcp.project.McpBackend;
import jadx.mcp.project.ProjectGate;
import jadx.mcp.project.ProjectLease;
import jadx.mcp.project.UiState;
import jadx.mcp.tools.McpTool;
import jadx.mcp.tools.Schema;
import jadx.mcp.tools.ToolErrorCode;
import jadx.mcp.tools.ToolException;
import jadx.mcp.tools.ToolRegistry;

import static jadx.mcp.jadx.ToolSupport.page;
import static jadx.mcp.jadx.ToolSupport.textPageParams;
import static jadx.mcp.jadx.ToolSupport.withProject;

/**
 * Project state and GUI integration tools
 */
final class ProjectTools {

	private ProjectTools() {
	}

	static void register(ToolRegistry registry, McpBackend backend, BooleanSupplier writeEnabled) {
		registry.register(McpTool.builder("get_project_info")
				.title("Project info")
				.description("Get state of the project opened in jadx: load state, inputs, classes/resources counts, "
						+ "persistence status. Works also when no project is loaded. Call first to check that jadx is ready.")
				.input(Schema.object().build())
				.output(Schema.object().open()
						.required("state", Schema.enumOf("Project state", null, "EMPTY", "LOADING", "READY", "CLOSING"))
						.required("loaded", Schema.bool("Project ready for requests"))
						.required("generation", Schema.integer("Project load counter, changes on each reload"))
						.required("jadx_version", Schema.string("jadx version"))
						.required("gui", Schema.bool("Connected to jadx-gui (current tab, selection and navigation available)"))
						.required("write_tools_enabled", Schema.bool("Rename/comment tools enabled"))
						.optional("project", Schema.anyObject("Project details"))
						.optional("counts", Schema.anyObject("Classes, packages and resources counts"))
						.required("save", Schema.anyObject("Project file persistence status"))
						.build())
				.handler((ctx, args) -> getProjectInfo(backend, writeEnabled, ctx.getToken()))
				.build());

		registry.register(McpTool.builder("fetch_current_class")
				.title("Current class in GUI")
				.description("Get the class opened in the active jadx-gui editor tab with its decompiled source (paged). "
						+ "Line numbers refer to decompiled code shown in GUI.")
				.input(textPageParams(Schema.object()
						.optional("include_source", Schema.bool("Include decompiled source", true)))
								.build())
				.output(Schema.object().open()
						.required("tab", tabSchema())
						.required("class", nullable(NodeJson.classSummarySchema(), "Class in active tab, null if not a class tab"))
						.optional("source", NodeJson.pageSchema("Source page"))
						.optional("message", Schema.string("Hint if no class opened"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					UiState ui = backend.getUiState(lease, ctx);
					JsonObject result = new JsonObject();
					result.add("tab", tabJson(ui));
					JavaNode node = ui.getTabNode();
					if (!(node instanceof JavaClass)) {
						result.add("class", null);
						result.addProperty("message", ui.getTabKind().equals("none")
								? "No tab opened in jadx-gui"
								: "Active tab is not a decompiled class: " + ui.getTabTitle());
						return result;
					}
					JavaClass cls = (JavaClass) node;
					result.add("class", NodeJson.classSummary(cls));
					if (args.getBool("include_source", true)) {
						CodeText code = new CodeText(cls.getTopParentClass().getCode());
						lease.check();
						result.add("source", page(code, args));
					}
					return result;
				}))
				.build());

		registry.register(McpTool.builder("get_selected_text")
				.title("Selection in GUI")
				.description("Get text selected in the active jadx-gui editor, caret position and code nodes under caret. "
						+ "Use to work with what the user is looking at.")
				.input(Schema.object().build())
				.output(Schema.object().open()
						.required("tab", tabSchema())
						.required("has_selection", Schema.bool("Text selected"))
						.required("selected_text", Schema.nullableString("Selected text"))
						.required("start_offset", Schema.integer("Selection start offset, -1 if none"))
						.required("end_offset", Schema.integer("Selection end offset, -1 if none"))
						.required("start_line", Schema.integer("Selection start line (1-based), -1 if unknown"))
						.required("end_line", Schema.integer("Selection end line, -1 if unknown"))
						.required("caret_line", Schema.integer("Caret line, -1 if unknown"))
						.required("class", nullable(NodeJson.classSummarySchema(), "Class in active tab"))
						.required("node_under_caret", NodeJson.nullableRefSchema("Code node (class/method/field/variable) under caret"))
						.required("enclosing_node", NodeJson.nullableRefSchema("Method or class containing caret"))
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					UiState ui = backend.getUiState(lease, ctx);
					JsonObject result = new JsonObject();
					result.add("tab", tabJson(ui));
					String selected = ui.getSelectedText();
					boolean hasSelection = selected != null && !selected.isEmpty();
					result.addProperty("has_selection", hasSelection);
					result.addProperty("selected_text", hasSelection ? selected : null);
					result.addProperty("start_offset", hasSelection ? ui.getSelectionStart() : -1);
					result.addProperty("end_offset", hasSelection ? ui.getSelectionEnd() : -1);
					result.addProperty("start_line", hasSelection ? ui.getSelectionStartLine() : -1);
					result.addProperty("end_line", hasSelection ? ui.getSelectionEndLine() : -1);
					result.addProperty("caret_line", ui.getCaretLine());
					JavaNode tabNode = ui.getTabNode();
					result.add("class", tabNode instanceof JavaClass ? NodeJson.classSummary((JavaClass) tabNode) : null);
					JavaNode under = ui.getNodeUnderCaret();
					result.add("node_under_caret", under == null ? null : NodeJson.nodeRef(under));
					JavaNode enclosing = ui.getEnclosingNode();
					result.add("enclosing_node", enclosing == null ? null : NodeJson.nodeRef(enclosing));
					return result;
				}))
				.build());

		registry.register(McpTool.builder("open_in_gui")
				.title("Open in GUI")
				.description("Open class, method, field or package in jadx-gui editor (code jump) so the user can see it. "
						+ "Optionally scroll to a line of the decompiled class code.")
				.input(Schema.object()
						.required("node_id", Schema.nonEmptyString("Node id from other tools (class, method, field) or class full name"))
						.optional("line", Schema.integer("Line in decompiled code of top-level class to scroll to (1-based)",
								1, Integer.MAX_VALUE, null))
						.build())
				.output(Schema.object().open()
						.required("opened", Schema.bool("Navigation requested in GUI"))
						.required("node", NodeJson.nodeRefSchema())
						.build())
				.handler((ctx, args) -> withProject(backend, ctx, (lease, resolver) -> {
					if (!backend.hasUi()) {
						throw new ToolException(ToolErrorCode.UNSUPPORTED, "GUI is not available");
					}
					JavaNode node = resolver.resolveById(args.getRequiredString("node_id"));
					backend.openInUi(lease, node, args.getOptInt("line"), ctx);
					JsonObject result = new JsonObject();
					result.addProperty("opened", true);
					result.add("node", NodeJson.nodeRef(node));
					return result;
				}))
				.build());
	}

	private static JsonObject getProjectInfo(McpBackend backend, BooleanSupplier writeEnabled,
			jadx.mcp.tools.CancellationToken token) {
		ProjectGate gate = backend.getGate();
		JsonObject result = new JsonObject();
		result.addProperty("jadx_version", JadxDecompiler.getVersion());
		result.addProperty("gui", backend.hasUi());
		result.addProperty("write_tools_enabled", writeEnabled.getAsBoolean());
		ProjectLease lease;
		try {
			lease = gate.acquire(token);
		} catch (ToolException e) {
			result.addProperty("state", gate.getState().name());
			result.addProperty("loaded", false);
			result.addProperty("generation", gate.getGeneration());
			result.add("save", backend.getSaveStatus().toJson());
			result.addProperty("message", e.getMessage());
			return result;
		}
		try (lease) {
			result.addProperty("state", ProjectGate.State.READY.name());
			result.addProperty("loaded", true);
			result.addProperty("generation", lease.getGeneration());
			JsonObject project = new JsonObject();
			backend.fillProjectInfo(lease, project);
			JadxDecompiler decompiler = lease.getDecompiler();
			JsonArray inputs = new JsonArray();
			decompiler.getArgs().getInputFiles().forEach(f -> inputs.add(f.getAbsolutePath()));
			project.add("inputs", inputs);
			result.add("project", project);
			JsonObject counts = new JsonObject();
			counts.addProperty("classes", decompiler.getClasses().size());
			counts.addProperty("classes_with_inner", decompiler.getRoot().getClasses().size());
			counts.addProperty("packages", decompiler.getPackages().size());
			counts.addProperty("resources", decompiler.getResources().size());
			result.add("counts", counts);
			result.add("save", backend.getSaveStatus().toJson());
		}
		return result;
	}

	static JsonObject tabJson(UiState ui) {
		JsonObject tab = new JsonObject();
		tab.addProperty("title", ui.getTabTitle());
		tab.addProperty("kind", ui.getTabKind());
		tab.addProperty("resource", ui.getResourceName());
		return tab;
	}

	static JsonObject tabSchema() {
		return Schema.object().open()
				.required("title", Schema.nullableString("Tab title"))
				.required("kind", Schema.enumOf("Tab content kind", null, "code", "smali", "resource", "other", "none"))
				.required("resource", Schema.nullableString("Resource file name for resource tabs"))
				.build();
	}

	static JsonObject nullable(JsonObject schema, String description) {
		JsonArray types = new JsonArray();
		types.add("object");
		types.add("null");
		schema.add("type", types);
		schema.addProperty("description", description);
		return schema;
	}
}
