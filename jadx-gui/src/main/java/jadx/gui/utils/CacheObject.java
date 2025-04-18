package jadx.gui.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.gui.JadxWrapper;
import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.utils.pkgs.PackageHelper;

public class CacheObject {
	private final JadxWrapper wrapper;
	private final JNodeCache jNodeCache;
	private final PackageHelper packageHelper;

	private String lastSearch;
	private Map<SearchDialog.SearchPreset, Set<SearchDialog.SearchOptions>> lastSearchOptions;
	private String lastSearchPackage;

	private volatile boolean fullDecompilationFinished;

	public CacheObject(JadxWrapper wrapper) {
		this.wrapper = wrapper;
		this.jNodeCache = new JNodeCache(wrapper);
		this.packageHelper = new PackageHelper(wrapper, jNodeCache);
		reset();
	}

	public void reset() {
		lastSearch = null;
		jNodeCache.reset();
		lastSearchOptions = new HashMap<>();
		lastSearchPackage = null;
		fullDecompilationFinished = false;
	}

	@Nullable
	public String getLastSearch() {
		return lastSearch;
	}

	@Nullable
	public String getLastSearchPackage() {
		return lastSearchPackage;
	}

	public void setLastSearch(String lastSearch) {
		this.lastSearch = lastSearch;
	}

	public void setLastSearchPackage(String lastSearchPackage) {
		this.lastSearchPackage = lastSearchPackage;
	}

	public JNodeCache getNodeCache() {
		return jNodeCache;
	}

	public Map<SearchDialog.SearchPreset, Set<SearchDialog.SearchOptions>> getLastSearchOptions() {
		return lastSearchOptions;
	}

	public PackageHelper getPackageHelper() {
		return packageHelper;
	}

	public boolean isFullDecompilationFinished() {
		return fullDecompilationFinished;
	}

	public void setFullDecompilationFinished(boolean fullDecompilationFinished) {
		this.fullDecompilationFinished = fullDecompilationFinished;
	}
}
