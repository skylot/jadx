package jadx.gui.search;

import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaPackage;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JResource;

public class SearchSettings {

	private final String searchString;
	private final boolean useRegex;
	private final boolean ignoreCase;
	private final boolean wholeWord;
	private final JavaPackage searchPackage;

	private JClass activeCls;
	private JResource activeResource;
	private Pattern regexPattern;
	private ISearchMethod searchMethod;

	public SearchSettings(String searchString, boolean ignoreCase, boolean wholeWord, boolean useRegex, JavaPackage searchPackage) {
		this.searchString = searchString;
		this.ignoreCase = ignoreCase;
		this.wholeWord = wholeWord;
		this.useRegex = useRegex;
		this.searchPackage = searchPackage;
	}

	@Nullable
	public String prepare() {
		try {
			this.regexPattern = SearchManager.generatePattern(searchString, ignoreCase, wholeWord, useRegex);
		} catch (Exception e) {
			return "Invalid Regex: " + e.getMessage();
		}

		searchMethod = ISearchMethod.build(this);
		return null;
	}

	public boolean isMatch(String searchArea) {
		return searchMethod.find(searchArea, 0) != null;
	}

	public JavaPackage getSearchPackage() {
		return this.searchPackage;
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
