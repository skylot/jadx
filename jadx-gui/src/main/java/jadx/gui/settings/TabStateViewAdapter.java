package jadx.gui.settings;

import org.jetbrains.annotations.Nullable;

import jadx.api.JavaClass;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.settings.data.ViewPoint;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;

public class TabStateViewAdapter {

	@Nullable
	public static TabViewState build(EditorViewState viewState) {
		TabViewState tvs = new TabViewState();
		if (!saveJNode(tvs, viewState.getNode())) {
			return null;
		}
		tvs.setSubPath(viewState.getSubPath());
		tvs.setCaret(viewState.getCaretPos());
		tvs.setView(new ViewPoint(viewState.getViewPoint()));
		return tvs;
	}

	@Nullable
	public static EditorViewState load(MainWindow mw, TabViewState tvs) {
		JNode node = loadJNode(mw, tvs);
		if (node == null) {
			return null;
		}
		return new EditorViewState(node, tvs.getSubPath(), tvs.getCaret(), tvs.getView().toPoint());
	}

	@Nullable
	private static JNode loadJNode(MainWindow mw, TabViewState tvs) {
		if ("class".equals(tvs.getType())) {
			JavaClass javaClass = mw.getWrapper().searchJavaClassByRawName(tvs.getTabPath());
			if (javaClass != null) {
				return mw.getCacheObject().getNodeCache().makeFrom(javaClass);
			}
		}
		return null;
	}

	private static boolean saveJNode(TabViewState tvs, JNode node) {
		if (node instanceof JClass) {
			tvs.setType("class");
			tvs.setTabPath(((JClass) node).getCls().getRawName());
			return true;
		}
		return false;
	}
}
