package jadx.gui.settings.data;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import jadx.api.data.impl.JadxCodeData;

public class ProjectData {
	private int projectVersion = 2;
	private List<Path> files = new ArrayList<>();
	private List<String> treeExpansionsV2 = new ArrayList<>();
	private JadxCodeData codeData = new JadxCodeData();
	private List<TabViewState> openTabs = Collections.emptyList();
	private @Nullable Path mappingsPath;
	private @Nullable String cacheDir; // don't use relative path adapter
	private boolean enableLiveReload = false;
	private List<String> searchHistory = new ArrayList<>();
	private String searchResourcesFilter = "*";
	private int searchResourcesSizeLimit = 0; // in MB

	protected Map<String, String> pluginOptions = new HashMap<>();

	public List<Path> getFiles() {
		return files;
	}

	public void setFiles(List<Path> files) {
		this.files = Objects.requireNonNull(files);
	}

	public List<String> getTreeExpansionsV2() {
		return treeExpansionsV2;
	}

	public void setTreeExpansionsV2(List<String> treeExpansionsV2) {
		this.treeExpansionsV2 = treeExpansionsV2;
	}

	public JadxCodeData getCodeData() {
		return codeData;
	}

	public void setCodeData(JadxCodeData codeData) {
		this.codeData = codeData;
	}

	public int getProjectVersion() {
		return projectVersion;
	}

	public void setProjectVersion(int projectVersion) {
		this.projectVersion = projectVersion;
	}

	public List<TabViewState> getOpenTabs() {
		return openTabs;
	}

	public boolean setOpenTabs(List<TabViewState> openTabs) {
		if (this.openTabs.equals(openTabs)) {
			return false;
		}
		this.openTabs = openTabs;
		return true;
	}

	@Nullable
	public Path getMappingsPath() {
		return mappingsPath;
	}

	public void setMappingsPath(Path mappingsPath) {
		this.mappingsPath = mappingsPath;
	}

	public @Nullable String getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(@Nullable String cacheDir) {
		this.cacheDir = cacheDir;
	}

	public boolean isEnableLiveReload() {
		return enableLiveReload;
	}

	public void setEnableLiveReload(boolean enableLiveReload) {
		this.enableLiveReload = enableLiveReload;
	}

	public List<String> getSearchHistory() {
		return searchHistory;
	}

	public void setSearchHistory(List<String> searchHistory) {
		this.searchHistory = searchHistory;
	}

	public String getSearchResourcesFilter() {
		return searchResourcesFilter;
	}

	public void setSearchResourcesFilter(String searchResourcesFilter) {
		this.searchResourcesFilter = searchResourcesFilter;
	}

	public int getSearchResourcesSizeLimit() {
		return searchResourcesSizeLimit;
	}

	public void setSearchResourcesSizeLimit(int searchResourcesSizeLimit) {
		this.searchResourcesSizeLimit = searchResourcesSizeLimit;
	}

	public Map<String, String> getPluginOptions() {
		return pluginOptions;
	}
}
