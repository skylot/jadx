package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenu;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.plugins.PluginContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodePopupBuilder;
import jadx.gui.utils.ui.ActionHandler;

public class CommonGuiPluginsContext {
	private static final Logger LOG = LoggerFactory.getLogger(CommonGuiPluginsContext.class);

	private final MainWindow mainWindow;
	private final Map<PluginContext, GuiPluginContext> pluginsMap = new HashMap<>();

	private final List<CodePopupAction> codePopupActionList = new ArrayList<>();

	public CommonGuiPluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public GuiPluginContext buildForPlugin(PluginContext pluginContext) {
		GuiPluginContext guiPluginContext = new GuiPluginContext(this, pluginContext);
		pluginsMap.put(pluginContext, guiPluginContext);
		return guiPluginContext;
	}

	public @Nullable GuiPluginContext getPluginGuiContext(PluginContext pluginContext) {
		return pluginsMap.get(pluginContext);
	}

	public void reset() {
		codePopupActionList.clear();
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.removeAll();
		pluginsMenu.setVisible(false);
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public List<CodePopupAction> getCodePopupActionList() {
		return codePopupActionList;
	}

	public void addMenuAction(String name, Runnable action) {
		ActionHandler item = new ActionHandler(ev -> {
			try {
				mainWindow.getBackgroundExecutor().execute(name, action);
			} catch (Exception e) {
				LOG.error("Error running action for menu item: {}", name, e);
			}
		});
		item.setNameAndDesc(name);
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.add(item);
		pluginsMenu.setVisible(true);
	}

	public void appendPopupMenus(CodeArea codeArea, JNodePopupBuilder popup) {
		if (codePopupActionList.isEmpty()) {
			return;
		}
		popup.addSeparator();
		for (CodePopupAction codePopupAction : codePopupActionList) {
			popup.add(codePopupAction.buildAction(codeArea));
		}
	}
}
