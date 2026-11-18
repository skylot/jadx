package jadx.gui.search;

import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.core.dex.info.PackageInfo;
import jadx.core.dex.nodes.PackageNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.utils.exceptions.InvalidDataException;
import jadx.gui.search.providers.ResourceFilter;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

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
	private @Nullable PackageInfo searchAliasPkg;
	private @Nullable PackageInfo searchRawPkg;
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
			RootNode root = mainWindow.getWrapper().getDecompiler().getRoot();
			resolveSearchPackage(root, searchPkgStr);
			if (!hasSearchPackage()) {
				return NLS.str("search_dialog.package_not_found");
			}
		}
		searchMethod = ISearchMethod.build(this);
		try {
			resourceFilter = ResourceFilter.parse(resFilterStr);
		} catch (InvalidDataException e) {
			return "Invalid resource file filter: " + e.getMessage();
		}
		return null;
	}

	// renamed and original packages trees can differ, so search in both
	private void resolveSearchPackage(RootNode root, String pkgName) {
		for (PackageNode pkg : root.getPackages()) {
			PackageInfo aliasPkg = findInPath(pkg.getAliasPkgInfo(), pkgName);
			if (aliasPkg != null) {
				searchAliasPkg = aliasPkg;
				break;
			}
		}
		PackageNode rawPkg = root.resolvePackage(pkgName);
		if (rawPkg != null) {
			searchRawPkg = rawPkg.getPkgInfo();
		}
	}

	private static @Nullable PackageInfo findInPath(PackageInfo pkg, String fullName) {
		PackageInfo current = pkg;
		while (current != null) {
			if (current.getFullName().equals(fullName)) {
				return current;
			}
			current = current.getParentPkg();
		}
		return null;
	}

	private static boolean isInPath(PackageInfo pkg, @Nullable PackageInfo searchPkg) {
		if (searchPkg == null) {
			return false;
		}
		PackageInfo current = pkg;
		while (current != null) {
			if (current.equals(searchPkg)) {
				return true;
			}
			current = current.getParentPkg();
		}
		return false;
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

	public boolean hasSearchPackage() {
		return searchAliasPkg != null || searchRawPkg != null;
	}

	public boolean isInSearchPkg(JavaClass cls) {
		// inner classes keep original package node, use top class one
		PackageNode pkg = cls.getClassNode().getTopParentClass().getPackageNode();
		return isInPath(pkg.getAliasPkgInfo(), searchAliasPkg)
				|| isInPath(pkg.getPkgInfo(), searchRawPkg);
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
