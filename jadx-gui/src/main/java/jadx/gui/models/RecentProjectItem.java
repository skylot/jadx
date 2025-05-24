package jadx.gui.models;

import java.nio.file.Path;
import java.util.Objects;

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

	public String getFileName() {
		return path.getFileName().toString();
	}

	public String getAbsolutePath() {
		return path.toAbsolutePath().toString();
	}

	@Override
	public String toString() {
		return getFileName();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		RecentProjectItem that = (RecentProjectItem) o;
		return path.equals(that.path);
	}

	@Override
	public int hashCode() {
		return Objects.hash(path);
	}
}
