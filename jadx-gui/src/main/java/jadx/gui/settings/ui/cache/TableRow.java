package jadx.gui.settings.ui.cache;

import java.nio.file.Paths;

import jadx.api.plugins.utils.CommonFileUtils;
import jadx.gui.cache.manager.CacheEntry;

final class TableRow {
	private final CacheEntry cacheEntry;
	private final String project;
	private String usage;
	private boolean selected = false;

	public TableRow(CacheEntry cacheEntry) {
		this.cacheEntry = cacheEntry;
		this.project = cutProjectName(cacheEntry.getProject());
		this.usage = "-";
	}

	private String cutProjectName(String project) {
		if (project.startsWith("tmp:")) {
			int hashStart = project.lastIndexOf('-');
			int endIdx = hashStart != -1 ? hashStart : project.length();
			return project.substring(4, endIdx) + " (Temp)";
		}
		return CommonFileUtils.removeFileExtension(Paths.get(project).getFileName().toString());
	}

	public CacheEntry getCacheEntry() {
		return cacheEntry;
	}

	public String getProject() {
		return project;
	}

	public String getUsage() {
		return usage;
	}

	public void setUsage(String usage) {
		this.usage = usage;
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}
}
