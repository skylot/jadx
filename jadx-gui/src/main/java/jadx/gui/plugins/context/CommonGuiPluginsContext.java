package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPluginInfo;
import jadx.core.plugins.PluginContext;
import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodePopupBuilder;
import jadx.gui.utils.ui.ActionHandler;

public class CommonGuiPluginsContext {
	private static final Logger LOG = LoggerFactory.getLogger(CommonGuiPluginsContext.class);

	private final MainWindow mainWindow;

	private final GuiPluginsRegistry appScope = new GuiPluginsRegistry();
	private final GuiPluginsRegistry projectScope = new GuiPluginsRegistry();

	private final List<GuiPluginContext> appPlugins = new ArrayList<>();
	private final Map<PluginContext, GuiPluginContext> pluginsMap = new HashMap<>();

	public CommonGuiPluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public GuiPluginContext buildForPlugin(PluginContext pluginContext) {
		GuiPluginContext guiPluginContext = new GuiPluginContext(this, projectScope, pluginContext.getPluginId(), pluginContext);
		pluginsMap.put(pluginContext, guiPluginContext);
		return guiPluginContext;
	}

	public GuiPluginContext buildForAppPlugin(JadxPluginInfo pluginInfo) {
		GuiPluginContext guiPluginContext = new GuiPluginContext(this, appScope, pluginInfo.getPluginId(), null);
		appPlugins.add(guiPluginContext);
		return guiPluginContext;
	}

	public @Nullable GuiPluginContext getPluginGuiContext(PluginContext pluginContext) {
		return pluginsMap.get(pluginContext);
	}

	public @Nullable GuiPluginContext getGuiPluginContextById(String pluginId) {
		for (GuiPluginContext guiPluginContext : appPlugins) {
			if (guiPluginContext.getPluginId().equals(pluginId)) {
				return guiPluginContext;
			}
		}
		for (GuiPluginContext guiPluginContext : pluginsMap.values()) {
			if (guiPluginContext.getPluginId().equals(pluginId)) {
				return guiPluginContext;
			}
		}
		return null;
	}

	public List<GuiPluginContext> getAppPluginContexts() {
		return appPlugins;
	}

	public void resetProjectScope() {
		projectScope.clear();
		pluginsMap.clear();
		mainWindow.resetPluginsMenu();
		for (Action menuAction : appScope.getMenuActions()) {
			mainWindow.addToPluginsMenu(menuAction);
		}
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public List<CodePopupAction> getCodePopupActionList() {
		return merge(appScope.getCodePopupActions(), projectScope.getCodePopupActions());
	}

	public List<TreePopupMenuEntry> getTreePopupMenuEntries() {
		return merge(appScope.getTreePopupMenuEntries(), projectScope.getTreePopupMenuEntries());
	}

	public List<ITreeInputCategory> getTreeInputCategories() {
		return merge(appScope.getTreeInputCategories(), projectScope.getTreeInputCategories());
	}

	public List<ITabStatePersist> getTabStatePersistAdapters() {
		return merge(appScope.getTabStatePersistAdapters(), projectScope.getTabStatePersistAdapters());
	}

	void addMenuAction(GuiPluginsRegistry registry, String name, Runnable action) {
		ActionHandler item = new ActionHandler(ev -> {
			try {
				mainWindow.getBackgroundExecutor().execute(name, action);
			} catch (Exception e) {
				LOG.error("Error running action for menu item: {}", name, e);
			}
		});
		item.setNameAndDesc(name);
		registry.getMenuActions().add(item);
		mainWindow.addToPluginsMenu(item);
	}

	public void appendPopupMenus(CodeArea codeArea, JNodePopupBuilder popup) {
		List<CodePopupAction> codePopupActionList = getCodePopupActionList();
		if (codePopupActionList.isEmpty()) {
			return;
		}
		popup.addSeparator();
		for (CodePopupAction codePopupAction : codePopupActionList) {
			popup.add(codePopupAction.buildAction(codeArea));
		}
	}

	private static <T> List<T> merge(List<T> appList, List<T> projectList) {
		if (appList.isEmpty()) {
			return projectList;
		}
		if (projectList.isEmpty()) {
			return appList;
		}
		List<T> result = new ArrayList<>(appList.size() + projectList.size());
		result.addAll(appList);
		result.addAll(projectList);
		return result;
	}
}
