package jadx.gui.settings;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.gui.plugins.mappings.JInputMapping;
import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.settings.data.ViewPoint;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JSubResource;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.UiUtils;

public class TabStateViewAdapter {
	private static final Logger LOG = LoggerFactory.getLogger(TabStateViewAdapter.class);

	private final Map<String, ITabStatePersist> customAdaptersMap = new HashMap<>();

	public @Nullable TabViewState build(EditorViewState viewState) {
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

	public @Nullable EditorViewState load(MainWindow mw, TabViewState tvs) {
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

	public void setCustomAdapters(List<ITabStatePersist> customAdapters) {
		customAdaptersMap.clear();
		for (ITabStatePersist customAdapter : customAdapters) {
			customAdaptersMap.put(customAdapter.getNodeClass().getName(), customAdapter);
		}
	}

	@Nullable
	private JNode loadJNode(MainWindow mw, TabViewState tvs) {
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

			case "mapping":
				return mw.getTreeRoot().followStaticPath("JInputs").searchNode(node -> node instanceof JInputMapping);
		}
		ITabStatePersist statePersist = customAdaptersMap.get(tvs.getType());
		if (statePersist != null) {
			try {
				return statePersist.load(tvs.getTabPath());
			} catch (Exception e) {
				LOG.error("Failed to restore tab for custom node adapter: {}", tvs.getType(), e);
			}
		}
		return null;
	}

	private boolean saveJNode(TabViewState tvs, JNode node) {
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
		if (node instanceof JInputMapping) {
			tvs.setType("mapping");
			return true;
		}

		String typeName = node.getClass().getName();
		ITabStatePersist statePersist = customAdaptersMap.get(typeName);
		if (statePersist != null) {
			try {
				tvs.setTabPath(statePersist.save(node));
				tvs.setType(statePersist.getNodeClass().getName());
				return true;
			} catch (Exception e) {
				LOG.error("Failed to save state for custom node: {}", typeName, e);
			}
		}
		return false;
	}
}
