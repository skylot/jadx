package jadx.gui.search.providers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.core.dex.nodes.ICodeNode;
import jadx.gui.search.ISearchMethod;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;

public abstract class BaseSearchProvider implements ISearchProvider {

	private final JNodeCache nodeCache;
	private final JadxDecompiler decompiler;
	protected final ISearchMethod searchMth;
	protected final String searchStr;
	protected final List<JavaClass> classes;
	protected final SearchSettings searchSettings;

	public BaseSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		this.nodeCache = mw.getCacheObject().getNodeCache();
		this.decompiler = mw.getWrapper().getDecompiler();
		this.searchMth = searchSettings.getSearchMethod();
		this.searchStr = searchSettings.getSearchString();
		if (searchSettings.getSearchPackage() != null) {
			this.classes = classes
					.stream()
					.filter(c -> c.getJavaPackage().isDescendantOf(searchSettings.getSearchPackage()))
					.collect(Collectors.toList());
		} else {
			this.classes = classes;
		}
		this.searchSettings = searchSettings;
	}

	protected boolean isMatch(String str) {
		return searchMth.find(str, searchStr, 0) != -1;
	}

	protected JNode convert(JavaNode node) {
		return nodeCache.makeFrom(node);
	}

	protected JClass convert(JavaClass cls) {
		return nodeCache.makeFrom(cls);
	}

	protected JNode convert(ICodeNode codeNode) {
		JavaNode node = Objects.requireNonNull(decompiler.getJavaNodeByRef(codeNode));
		return Objects.requireNonNull(nodeCache.makeFrom(node));
	}

	@Override
	public int total() {
		return classes.size();
	}
}
