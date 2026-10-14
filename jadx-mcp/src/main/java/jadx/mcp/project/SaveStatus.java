package jadx.mcp.project;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

/**
 * Persistence state of project data (renames, comments)
 */
public final class SaveStatus {
	private final boolean saved;
	private final @Nullable String projectFile;
	private final String autoSave;
	private final @Nullable String message;

	public SaveStatus(boolean saved, @Nullable String projectFile, String autoSave, @Nullable String message) {
		this.saved = saved;
		this.projectFile = projectFile;
		this.autoSave = autoSave;
		this.message = message;
	}

	/**
	 * True only if all changes are written to project file
	 */
	public boolean isSaved() {
		return saved;
	}

	public @Nullable String getProjectFile() {
		return projectFile;
	}

	public String getAutoSave() {
		return autoSave;
	}

	public @Nullable String getMessage() {
		return message;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("persisted", saved);
		obj.addProperty("project_file", projectFile);
		obj.addProperty("auto_save", autoSave);
		obj.addProperty("message", message);
		return obj;
	}
}
