package jadx.gui.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import jadx.gui.jobs.DecompileJob;
import jadx.gui.jobs.IndexJob;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.SearchDialog;
import jadx.gui.utils.search.CommentsIndex;
import jadx.gui.utils.search.TextSearchIndex;

public class CacheObject {

	private DecompileJob decompileJob;
	private IndexJob indexJob;

	private TextSearchIndex textIndex;
	private CodeUsageInfo usageInfo;
	private CommentsIndex commentsIndex;
	private String lastSearch;
	private JNodeCache jNodeCache;
	private Map<SearchDialog.SearchPreset, Set<SearchDialog.SearchOptions>> lastSearchOptions;

	private JRoot jRoot;
	private JadxSettings settings;

	public CacheObject() {
		reset();
	}

	public void reset() {
		jRoot = null;
		settings = null;
		decompileJob = null;
		indexJob = null;
		textIndex = null;
		lastSearch = null;
		jNodeCache = new JNodeCache();
		usageInfo = null;
		lastSearchOptions = new HashMap<>();
	}

	public DecompileJob getDecompileJob() {
		return decompileJob;
	}

	public void setDecompileJob(DecompileJob decompileJob) {
		this.decompileJob = decompileJob;
	}

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

	public CommentsIndex getCommentsIndex() {
		return commentsIndex;
	}

	public void setCommentsIndex(CommentsIndex commentsIndex) {
		this.commentsIndex = commentsIndex;
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
}
