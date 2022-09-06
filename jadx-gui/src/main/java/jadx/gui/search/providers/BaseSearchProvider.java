package jadx.gui.search.providers;

import java.util.List;

import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.gui.search.ISearchMethod;
import jadx.gui.search.ISearchProvider;
import jadx.gui.search.SearchSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.JNodeCache;

public abstract class BaseSearchProvider implements ISearchProvider {

	private final JNodeCache nodeCache;
	protected final ISearchMethod searchMth;
	protected final String searchStr;
	protected final List<JavaClass> classes;

	public BaseSearchProvider(MainWindow mw, SearchSettings searchSettings, List<JavaClass> classes) {
		this.nodeCache = mw.getCacheObject().getNodeCache();
		this.searchMth = searchSettings.getSearchMethod();
		this.searchStr = searchSettings.getSearchString();
		this.classes = classes;
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

	@Override
	public int total() {
		return classes.size();
	}
}
