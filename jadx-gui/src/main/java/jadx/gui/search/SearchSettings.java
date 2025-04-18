package jadx.gui.search;

import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.core.dex.nodes.PackageNode;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class SearchSettings {
	private final String searchString;
	private final boolean useRegex;
	private final boolean ignoreCase;
	private final String searchPkgStr;

	private JClass activeCls;
	private JResource activeResource;
	private Pattern regexPattern;
	private ISearchMethod searchMethod;
	private JavaPackage searchPackage;

	public SearchSettings(String searchString, boolean ignoreCase, boolean useRegex, String searchPkgStr) {
		this.searchString = searchString;
		this.useRegex = useRegex;
		this.ignoreCase = ignoreCase;
		this.searchPkgStr = searchPkgStr;
	}

	@Nullable
	public String prepare(MainWindow mainWindow) {
		if (useRegex) {
			try {
				int flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
				this.regexPattern = Pattern.compile(searchString, flags);
			} catch (Exception e) {
				return "Invalid Regex: " + e.getMessage();
			}
		}
		if (!searchPkgStr.isBlank()) {
			JadxDecompiler decompiler = mainWindow.getWrapper().getDecompiler();
			PackageNode pkg = decompiler.getRoot().resolvePackage(searchPkgStr);
			if (pkg == null) {
				return NLS.str("search_dialog.package_not_found");
			}
			searchPackage = pkg.getJavaNode();
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

	public JavaPackage getSearchPackage() {
		return this.searchPackage;
	}

	public boolean isInSearchPkg(JavaClass cls) {
		return cls.getJavaPackage().isDescendantOf(searchPackage);
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
