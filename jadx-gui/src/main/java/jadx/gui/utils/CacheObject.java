package jadx.gui.utils;

import java.util.EnumSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.gui.jobs.DecompileJob;
import jadx.gui.jobs.IndexJob;
import jadx.gui.ui.SearchDialog;
import jadx.gui.utils.search.TextSearchIndex;

public class CacheObject {

	private DecompileJob decompileJob;
	private IndexJob indexJob;

	private TextSearchIndex textIndex;
	private CodeUsageInfo usageInfo;
	private String lastSearch;
	private JNodeCache jNodeCache;
	private Set<SearchDialog.SearchOptions> lastSearchOptions;

	public CacheObject() {
		reset();
	}

	public void reset() {
		decompileJob = null;
		indexJob = null;
		textIndex = null;
		lastSearch = null;
		jNodeCache = new JNodeCache();
		usageInfo = null;
		lastSearchOptions = EnumSet.noneOf(SearchDialog.SearchOptions.class);
	}

	public DecompileJob getDecompileJob() {
		return decompileJob;
	}

	public void setDecompileJob(DecompileJob decompileJob) {
		this.decompileJob = decompileJob;
	}

	@Nullable
	public TextSearchIndex getTextIndex() {
		return textIndex;
	}

	public void setTextIndex(TextSearchIndex textIndex) {
		this.textIndex = textIndex;
	}

	@Nullable
	public String getLastSearch() {
		return lastSearch;
	}

	public void setLastSearch(String lastSearch) {
		this.lastSearch = lastSearch;
	}

	@Nullable
	public CodeUsageInfo getUsageInfo() {
		return usageInfo;
	}

	public void setUsageInfo(@Nullable CodeUsageInfo usageInfo) {
		this.usageInfo = usageInfo;
	}

	public IndexJob getIndexJob() {
		return indexJob;
	}

	public void setIndexJob(IndexJob indexJob) {
		this.indexJob = indexJob;
	}

	public JNodeCache getNodeCache() {
		return jNodeCache;
	}

	public void setLastSearchOptions(Set<SearchDialog.SearchOptions> lastSearchOptions) {
		this.lastSearchOptions = lastSearchOptions;
	}

	public Set<SearchDialog.SearchOptions> getLastSearchOptions() {
		return lastSearchOptions;
	}
}
