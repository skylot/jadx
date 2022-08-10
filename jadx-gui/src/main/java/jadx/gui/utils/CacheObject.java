package jadx.gui.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.dialog.SearchDialog;

public class CacheObject {

	private String lastSearch;
	private JNodeCache jNodeCache;
	private Map<SearchDialog.SearchPreset, Set<SearchDialog.SearchOptions>> lastSearchOptions;

	private JRoot jRoot;
	private JadxSettings settings;

	private List<List<JavaClass>> decompileBatches;

	public CacheObject() {
		reset();
	}

	public void reset() {
		jRoot = null;
		settings = null;
		lastSearch = null;
		jNodeCache = new JNodeCache();
		lastSearchOptions = new HashMap<>();
		decompileBatches = null;
	}

	@Nullable
	public String getLastSearch() {
		return lastSearch;
	}

	public void setLastSearch(String lastSearch) {
		this.lastSearch = lastSearch;
	}

	public JNodeCache getNodeCache() {
		return jNodeCache;
	}

	public Map<SearchDialog.SearchPreset, Set<SearchDialog.SearchOptions>> getLastSearchOptions() {
		return lastSearchOptions;
	}

	public void setJadxSettings(JadxSettings settings) {
		this.settings = settings;
	}

	public JadxSettings getJadxSettings() {
		return this.settings;
	}

	public JRoot getJRoot() {
		return jRoot;
	}

	public void setJRoot(JRoot jRoot) {
		this.jRoot = jRoot;
	}

	public @Nullable List<List<JavaClass>> getDecompileBatches() {
		return decompileBatches;
	}

	public void setDecompileBatches(List<List<JavaClass>> decompileBatches) {
		this.decompileBatches = decompileBatches;
	}
}
