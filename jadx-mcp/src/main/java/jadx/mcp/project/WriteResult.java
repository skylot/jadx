package jadx.mcp.project;

import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Result of applied project modification
 */
public final class WriteResult {
	private final List<String> updatedClasses;
	private final SaveStatus saveStatus;
	private final @Nullable String warning;

	public WriteResult(List<String> updatedClasses, SaveStatus saveStatus, @Nullable String warning) {
		this.updatedClasses = updatedClasses;
		this.saveStatus = saveStatus;
		this.warning = warning;
	}

	public WriteResult(SaveStatus saveStatus) {
		this(Collections.emptyList(), saveStatus, null);
	}

	/**
	 * Raw names of top-level classes refreshed after change
	 */
	public List<String> getUpdatedClasses() {
		return updatedClasses;
	}

	public SaveStatus getSaveStatus() {
		return saveStatus;
	}

	public @Nullable String getWarning() {
		return warning;
	}
}
