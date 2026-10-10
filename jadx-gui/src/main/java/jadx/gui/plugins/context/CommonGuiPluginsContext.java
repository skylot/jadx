package jadx.gui.plugins.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Function;

import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.plugins.PluginRuntime;
import jadx.gui.settings.data.ITabStatePersist;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.JNodePopupBuilder;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ActionHandler;

public class CommonGuiPluginsContext {
	private static final Logger LOG = LoggerFactory.getLogger(CommonGuiPluginsContext.class);

	private final MainWindow mainWindow;

	private final GuiPluginsRegistry projectScope = new GuiPluginsRegistry();

	// each global plugin has own registry to allow unload
	private final Map<PluginRuntime, GuiPluginContext> globalPlugins = new ConcurrentSkipListMap<>();
	private final Map<PluginRuntime, GuiPluginContext> projectPlugins = new HashMap<>();

	// key bindings added to main window, changed only in UI thread
	private final Map<KeyStroke, KeyBindingEntry> appliedKeyBindings = new ConcurrentHashMap<>();

	public CommonGuiPluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public GuiPluginContext buildForPlugin(PluginRuntime pluginRuntime, boolean isGlobalPlugin) {
		if (isGlobalPlugin) {
			GuiPluginContext guiPluginContext = new GuiPluginContext(this, new GuiPluginsRegistry(), pluginRuntime);
			globalPlugins.put(pluginRuntime, guiPluginContext);
			return guiPluginContext;
		}
		GuiPluginContext guiPluginContext = new GuiPluginContext(this, projectScope, pluginRuntime);
		projectPlugins.put(pluginRuntime, guiPluginContext);
		return guiPluginContext;
	}

	public void copyGlobalPluginData(PluginRuntime globalContext, PluginRuntime projectContext) {
		GuiPluginContext globalGuiContext = globalPlugins.get(globalContext);
		GuiPluginContext projectGuiContext = projectPlugins.get(projectContext);
		if (globalGuiContext != null && projectGuiContext != null) {
			projectGuiContext.setCustomSettings(globalGuiContext.getCustomSettingsGroup());
		}
	}

	public void resetProjectScope() {
		projectScope.clear();
		projectPlugins.clear();
		updatePluginEntries();
	}

	public void removeGlobalPlugin(PluginRuntime pluginRuntime) {
		globalPlugins.remove(pluginRuntime);
		updatePluginEntries();
	}

	private void updatePluginEntries() {
		mainWindow.resetPluginsMenu();
		for (Action menuAction : collect(GuiPluginsRegistry::getMenuActions)) {
			mainWindow.addToPluginsMenu(menuAction);
		}
		List<KeyBindingEntry> keyBindings = collect(GuiPluginsRegistry::getKeyBindings);
		UiUtils.uiRun(() -> {
			JComponent mainPanel = getMainPanel();
			for (KeyBindingEntry keyBinding : appliedKeyBindings.values()) {
				getInputMap().remove(keyBinding.getKeyStroke());
				mainPanel.getActionMap().remove(keyBinding.getId());
			}
			appliedKeyBindings.clear();
			keyBindings.forEach(this::applyKeyBinding);
		});
	}

	boolean addKeyBinding(GuiPluginsRegistry registry, KeyBindingEntry keyBinding) {
		KeyStroke keyStroke = keyBinding.getKeyStroke();
		for (KeyBindingEntry registered : collect(GuiPluginsRegistry::getKeyBindings)) {
			if (registered.getKeyStroke().equals(keyStroke)) {
				return false;
			}
		}
		if (getInputMap().get(keyStroke) != null && !appliedKeyBindings.containsKey(keyStroke)) {
			// used by jadx-gui
			return false;
		}
		registry.getKeyBindings().add(keyBinding);
		UiUtils.uiRun(() -> applyKeyBinding(keyBinding));
		return true;
	}

	private void applyKeyBinding(KeyBindingEntry keyBinding) {
		getInputMap().put(keyBinding.getKeyStroke(), keyBinding.getId());
		getMainPanel().getActionMap().put(keyBinding.getId(), new ActionHandler(keyBinding.getAction()));
		appliedKeyBindings.put(keyBinding.getKeyStroke(), keyBinding);
	}

	private JComponent getMainPanel() {
		return (JComponent) mainWindow.getContentPane();
	}

	private InputMap getInputMap() {
		return getMainPanel().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	public MainWindow getMainWindow() {
		return mainWindow;
	}

	public List<CodePopupAction> getCodePopupActionList() {
		return collect(GuiPluginsRegistry::getCodePopupActions);
	}

	public List<TreePopupMenuEntry> getTreePopupMenuEntries() {
		return collect(GuiPluginsRegistry::getTreePopupMenuEntries);
	}

	public List<ITreeInputCategory> getTreeInputCategories() {
		return collect(GuiPluginsRegistry::getTreeInputCategories);
	}

	public List<ITabStatePersist> getTabStatePersistAdapters() {
		return collect(GuiPluginsRegistry::getTabStatePersistAdapters);
	}

	private <T> List<T> collect(Function<GuiPluginsRegistry, List<T>> getter) {
		List<T> list = new ArrayList<>();
		for (GuiPluginContext globalPlugin : globalPlugins.values()) {
			list.addAll(getter.apply(globalPlugin.getRegistry()));
		}
		list.addAll(getter.apply(projectScope));
		return list;
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

	boolean isGlobalPlugin(GuiPluginContext guiPluginContext) {
		return globalPlugins.containsValue(guiPluginContext);
	}
}
