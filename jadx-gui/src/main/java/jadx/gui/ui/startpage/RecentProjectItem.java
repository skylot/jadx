package jadx.gui.ui.startpage;

import java.nio.file.Path;
import java.util.Objects;

import jadx.api.plugins.utils.CommonFileUtils;

/**
 * Represents an item in the recent projects list.
 */
public class RecentProjectItem {
	private final Path path;

	public RecentProjectItem(Path path) {
		this.path = Objects.requireNonNull(path);
	}

	public Path getPath() {
		return path;
	}

	public String getProjectName() {
		return CommonFileUtils.removeFileExtension(path.getFileName().toString());
	}

	public String getAbsolutePath() {
		return path.toAbsolutePath().toString();
	}

	@Override
	public String toString() {
		return getProjectName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		RecentProjectItem that = (RecentProjectItem) o;
		return Objects.equals(path, that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}
}
