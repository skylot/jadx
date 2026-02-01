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
import jadx.gui.treemodel.JSubResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.UiUtils;

public class TabStateViewAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(TabStateViewAdapter.class);

	@Nullable
	public static TabViewState build(EditorViewState viewState) {
		TabViewState tvs = new TabViewState();
		tvs.setSubPath(viewState.getSubPath());
		if (!saveJNode(tvs, viewState.getNode())) {
			if (UiUtils.JADX_GUI_DEBUG) {
				LOG.warn("Can't save view state: {}", viewState);
			}
			return null;
		}
		tvs.setCaret(viewState.getCaretPos());
		tvs.setView(new ViewPoint(viewState.getViewPoint()));
		tvs.setActive(viewState.isActive());
		tvs.setPinned(viewState.isPinned());
		tvs.setBookmarked(viewState.isBookmarked());
		tvs.setHidden(viewState.isHidden());
		tvs.setPreviewTab(viewState.isPreviewTab());
		return tvs;
	}

	@Nullable
	public static EditorViewState load(MainWindow mw, TabViewState tvs) {
		try {
			JNode node = loadJNode(mw, tvs);
			if (node == null) {
				if (UiUtils.JADX_GUI_DEBUG) {
					LOG.warn("Can't restore view for {}", tvs);
				}
				return null;
			}
			EditorViewState viewState = new EditorViewState(node, tvs.getSubPath(), tvs.getCaret(), tvs.getView().toPoint());
			viewState.setActive(tvs.isActive());
			viewState.setPinned(tvs.isPinned());
			viewState.setBookmarked(tvs.isBookmarked());
			viewState.setHidden(tvs.isHidden());
			viewState.setPreviewTab(tvs.isPreviewTab());
			return viewState;
		} catch (Exception e) {
			LOG.error("Failed to load tab state: {}", tvs, e);
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
				return mw.getTreeRoot().searchResourceByName(tvs.getTabPath());

			case "sub-resource":
				String[] parts = tvs.getTabPath().split(JSubResource.SUB_RES_PREFIX);
				JResource baseRes = mw.getTreeRoot().searchResourceByName(parts[0]);
				if (baseRes != null) {
					String subName = parts[1];
					return baseRes.searchDepthNode(n -> n.getName().equals(subName)); // will load node before search
				}
				return null;

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
		if (node instanceof JSubResource) {
			JSubResource subRes = (JSubResource) node;
			tvs.setType("sub-resource");
			tvs.setTabPath(subRes.getBaseRes().getName() + JSubResource.SUB_RES_PREFIX + subRes.getName());
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
