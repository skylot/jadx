package jadx.gui.cache.manager;

import org.jetbrains.annotations.NotNull;

public class CacheEntry implements Comparable<CacheEntry> {

	private String project;
	private String cache;
	private long timestamp;

	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}

	public String getCache() {
		return cache;
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public int compareTo(@NotNull CacheEntry other) {
		// recent entries first
		return -Long.compare(timestamp, other.timestamp);
	}

	@Override
	public String toString() {
		return "CacheEntry{project=" + project + ", cache=" + cache + "}";
	}
}
