package jadx.mcp.project;

import java.util.List;
import java.util.function.Consumer;

import jadx.api.JavaClass;
import jadx.api.data.ICodeComment;

/**
 * Change of project comments list affecting one top-level class
 */
public final class CommentsUpdate {
	private final JavaClass topClass;
	private final Consumer<List<ICodeComment>> updater;

	public CommentsUpdate(JavaClass topClass, Consumer<List<ICodeComment>> updater) {
		this.topClass = topClass;
		this.updater = updater;
	}

	public JavaClass getTopClass() {
		return topClass;
	}

	/**
	 * Modify comments list (called once with mutable copy of current comments)
	 */
	public Consumer<List<ICodeComment>> getUpdater() {
		return updater;
	}
}
