package jadx.mcp.project;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import jadx.api.JavaNode;
import jadx.api.data.ICodeComment;
import jadx.mcp.tools.ToolCallContext;
import jadx.mcp.tools.ToolException;

/**
 * Host application integration (GUI or headless).
 * All methods are called from MCP worker threads, implementations must handle UI thread switching.
 */
public interface McpBackend {

	ProjectGate getGate();

	/**
	 * Add host specific project properties (name, project file, inputs)
	 */
	void fillProjectInfo(ProjectLease lease, JsonObject info);

	/**
	 * GUI available: current tab, selection and navigation tools work
	 */
	boolean hasUi();

	UiState getUiState(ProjectLease lease, ToolCallContext ctx) throws ToolException;

	/**
	 * Open node in GUI editor
	 *
	 * @param line 1-based line inside class code to scroll to, null to jump to node definition
	 */
	void openInUi(ProjectLease lease, JavaNode node, @Nullable Integer line, ToolCallContext ctx) throws ToolException;

	/**
	 * Apply rename and wait until project data and dependent code are updated
	 */
	WriteResult rename(ProjectLease lease, RenameRequest request, ToolCallContext ctx) throws ToolException;

	/**
	 * Apply comments change and wait for code refresh
	 */
	WriteResult updateComments(ProjectLease lease, CommentsUpdate update, ToolCallContext ctx) throws ToolException;

	List<ICodeComment> getComments(ProjectLease lease);

	SaveStatus getSaveStatus();

	/**
	 * Save project file if possible
	 */
	SaveStatus saveProject(ProjectLease lease, ToolCallContext ctx) throws ToolException;
}
