package jadx.gui.search;

import java.util.List;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.utils.ListUtils;
import jadx.core.utils.exceptions.InvalidDataException;
import jadx.gui.search.providers.ResourceFilter;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.pkgs.PackageHelper;

public class SearchSettings {
	private final String searchString;
	private boolean useRegex;
	private boolean ignoreCase;
	private String searchPkgStr;
	private String resFilterStr;
	private int resSizeLimit; // in MB

	private JClass activeCls;
	private JResource activeResource;
	private Pattern regexPattern;
	private ISearchMethod searchMethod;
	private @Nullable String searchPackage;
	private ResourceFilter resourceFilter;

	public SearchSettings(String searchString) {
		this.searchString = searchString;
	}

	public @Nullable String prepare(MainWindow mainWindow) {
		if (useRegex) {
			try {
				int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
				this.regexPattern = Pattern.compile(searchString, flags);
			} catch (Exception e) {
				return "Invalid Regex: " + e.getMessage();
			}
		}
		if (!searchPkgStr.isBlank()) {
			List<JavaPackage> packages = mainWindow.getWrapper().getPackages();
			if (ListUtils.noneMatch(packages, p -> PackageHelper.isPkgMatch(p.getFullName(), searchPkgStr))) {
				return NLS.str("search_dialog.package_not_found");
			}
			searchPackage = searchPkgStr;
		}
		searchMethod = ISearchMethod.build(this);
		try {
			resourceFilter = ResourceFilter.parse(resFilterStr);
		} catch (InvalidDataException e) {
			return "Invalid resource file filter: " + e.getMessage();
		}
		return null;
	}

	public boolean isMatch(String searchArea) {
		return searchMethod.find(searchArea, this.searchString, 0) != -1;
	}

	public boolean isUseRegex() {
		return this.useRegex;
	}

	public void setUseRegex(boolean useRegex) {
		this.useRegex = useRegex;
	}

	public boolean isIgnoreCase() {
		return this.ignoreCase;
	}

	public void setIgnoreCase(boolean ignoreCase) {
		this.ignoreCase = ignoreCase;
	}

	public @Nullable String getSearchPackage() {
		return this.searchPackage;
	}

	public boolean isInSearchPkg(JavaClass cls) {
		return PackageHelper.isPkgMatch(cls.getPackage(), searchPackage);
	}

	public void setSearchPkgStr(String searchPkgStr) {
		this.searchPkgStr = searchPkgStr;
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

	public void setResFilterStr(String resFilterStr) {
		this.resFilterStr = resFilterStr;
	}

	public ResourceFilter getResourceFilter() {
		return resourceFilter;
	}

	public int getResSizeLimit() {
		return resSizeLimit;
	}

	public void setResSizeLimit(int resSizeLimit) {
		this.resSizeLimit = resSizeLimit;
	}
}
