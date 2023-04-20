package jadx.gui.settings;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.gui.plugins.mappings.JInputMapping;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.settings.data.ViewPoint;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JInputScript;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;

public class TabStateViewAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(TabStateViewAdapter.class);

	@Nullable
	public static TabViewState build(EditorViewState viewState) {
		TabViewState tvs = new TabViewState();
		if (!saveJNode(tvs, viewState.getNode())) {
			LOG.debug("Can't save view state: " + viewState);
			return null;
		}
		tvs.setSubPath(viewState.getSubPath());
		tvs.setCaret(viewState.getCaretPos());
		tvs.setView(new ViewPoint(viewState.getViewPoint()));
		tvs.setActive(viewState.isActive());
		return tvs;
	}

	@Nullable
	public static EditorViewState load(MainWindow mw, TabViewState tvs) {
		try {
			JNode node = loadJNode(mw, tvs);
			if (node == null) {
				return null;
			}
			EditorViewState viewState = new EditorViewState(node, tvs.getSubPath(), tvs.getCaret(), tvs.getView().toPoint());
			viewState.setActive(tvs.isActive());
			return viewState;
		} catch (Exception e) {
			LOG.error("Failed to load tab state: " + tvs, e);
			return null;
		}
	}

	@Nullable
	private static JNode loadJNode(MainWindow mw, TabViewState tvs) {
		switch (tvs.getType()) {
			case "class":
				JavaClass javaClass = mw.getWrapper().searchJavaClassByRawName(tvs.getTabPath());
				if (javaClass != null) {
					return mw.getCacheObject().getNodeCache().makeFrom(javaClass);
				}
				break;

			case "resource":
				JResource tmpNode = new JResource(null, tvs.getTabPath(), JResource.JResType.FILE);
				return mw.getTreeRoot().searchNode(tmpNode); // equals method in JResource check only name

			case "script":
				return mw.getTreeRoot()
						.followStaticPath("JInputs", "JInputScripts")
						.searchNode(node -> node instanceof JInputScript && node.getName().equals(tvs.getTabPath()));

			case "mapping":
				return mw.getTreeRoot().followStaticPath("JInputs").searchNode(node -> node instanceof JInputMapping);
		}
		return null;
	}

	private static boolean saveJNode(TabViewState tvs, JNode node) {
		if (node instanceof JClass) {
			tvs.setType("class");
			tvs.setTabPath(((JClass) node).getCls().getRawName());
			return true;
		}
		if (node instanceof JResource) {
			tvs.setType("resource");
			tvs.setTabPath(node.getName());
			return true;
		}
		if (node instanceof JInputScript) {
			tvs.setType("script");
			tvs.setTabPath(node.getName());
			return true;
		}
		if (node instanceof JInputMapping) {
			tvs.setType("mapping");
			return true;
		}
		return false;
	}
}
