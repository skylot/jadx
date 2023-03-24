package jadx.gui.search;

import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JResource;

public class SearchSettings {

	private final String searchString;
	private final boolean useRegex;
	private final boolean ignoreCase;

	private JClass activeCls;
	private JResource activeResource;
	private Pattern regexPattern;
	private ISearchMethod searchMethod;

	public SearchSettings(String searchString, boolean ignoreCase, boolean useRegex) {
		this.searchString = searchString;
		this.useRegex = useRegex;
		this.ignoreCase = ignoreCase;
	}

	@Nullable
	public String prepare() {
		if (useRegex) {
			try {
				int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
				this.regexPattern = Pattern.compile(searchString, flags);
			} catch (Exception e) {
				return "Invalid Regex: " + e.getMessage();
			}
		}
		searchMethod = ISearchMethod.build(this);
		return null;
	}

	public boolean isMatch(String searchArea) {
		return searchMethod.find(searchArea, this.searchString, 0) != -1;
	}

	public boolean isUseRegex() {
		return this.useRegex;
	}

	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	public String getSearchString() {
		return this.searchString;
	}

	public Pattern getPattern() {
		return this.regexPattern;
	}

	public JClass getActiveCls() {
		return activeCls;
	}

	public void setActiveCls(JClass activeCls) {
		this.activeCls = activeCls;
	}

	public JResource getActiveResource() {
		return activeResource;
	}

	public void setActiveResource(JResource activeResource) {
		this.activeResource = activeResource;
	}

	public ISearchMethod getSearchMethod() {
		return searchMethod;
	}
}
