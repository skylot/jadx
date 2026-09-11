package jadx.gui.plugins.context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.plugins.PluginContext;
import jadx.core.utils.Utils;
import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodePopupBuilder;
import jadx.gui.utils.ui.ActionHandler;

public class CommonGuiPluginsContext {
	private static final Logger LOG = LoggerFactory.getLogger(CommonGuiPluginsContext.class);

	private final MainWindow mainWindow;

	private final GuiPluginsRegistry globalScope = new GuiPluginsRegistry();
	private final GuiPluginsRegistry projectScope = new GuiPluginsRegistry();

	private final Map<PluginContext, GuiPluginContext> globalPlugins = new HashMap<>();
	private final Map<PluginContext, GuiPluginContext> projectPlugins = new HashMap<>();

	public CommonGuiPluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public GuiPluginContext buildForPlugin(PluginContext pluginContext, boolean isGlobalPlugin) {
		GuiPluginsRegistry registry = isGlobalPlugin ? globalScope : projectScope;
		GuiPluginContext guiPluginContext = new GuiPluginContext(this, registry, pluginContext);
		(isGlobalPlugin ? globalPlugins : projectPlugins).put(pluginContext, guiPluginContext);
		return guiPluginContext;
	}

	public void copyGlobalPluginData(PluginContext globalContext, PluginContext projectContext) {
		GuiPluginContext globalGuiContext = globalPlugins.get(globalContext);
		GuiPluginContext projectGuiContext = projectPlugins.get(projectContext);
		if (globalGuiContext != null && projectGuiContext != null) {
			projectGuiContext.setCustomSettings(globalGuiContext.getCustomSettingsGroup());
		}
	}

	public void resetProjectScope() {
		projectScope.clear();
		projectPlugins.clear();
		mainWindow.resetPluginsMenu();
		for (Action menuAction : globalScope.getMenuActions()) {
			mainWindow.addToPluginsMenu(menuAction);
		}
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public List<CodePopupAction> getCodePopupActionList() {
		return Utils.mergeLists(globalScope.getCodePopupActions(), projectScope.getCodePopupActions());
	}

	public List<TreePopupMenuEntry> getTreePopupMenuEntries() {
		return Utils.mergeLists(globalScope.getTreePopupMenuEntries(), projectScope.getTreePopupMenuEntries());
	}

	public List<ITreeInputCategory> getTreeInputCategories() {
		return Utils.mergeLists(globalScope.getTreeInputCategories(), projectScope.getTreeInputCategories());
	}

	public List<ITabStatePersist> getTabStatePersistAdapters() {
		return Utils.mergeLists(globalScope.getTabStatePersistAdapters(), projectScope.getTabStatePersistAdapters());
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
}
