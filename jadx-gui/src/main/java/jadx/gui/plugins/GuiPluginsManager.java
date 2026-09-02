package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.gui.JadxGuiPlugin;
import jadx.core.plugins.PluginContext;
import jadx.core.plugins.versions.VerifyRequiredVersion;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

public class GuiPluginsManager {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsManager.class);

	private final MainWindow mainWindow;
	private final JadxExternalPluginsLoader pluginsLoader = new JadxExternalPluginsLoader();
	private final List<JadxGuiPlugin> initializedPlugins = new ArrayList<>();

	public GuiPluginsManager(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void load() {
		try {
			long start = System.currentTimeMillis();
			List<JadxPlugin> plugins = pluginsLoader.load(JadxGuiPlugin.class::isAssignableFrom);
			Set<String> disabledPlugins = mainWindow.getSettings().toJadxArgs().getDisabledPlugins();
			CommonGuiPluginsContext guiPluginsContext = mainWindow.getGuiPluginsContext();
			initializedPlugins.addAll(
					initPlugins(plugins, disabledPlugins, new VerifyRequiredVersion(), guiPluginsContext::buildForAppPlugin));
			if (LOG.isDebugEnabled()) {
				LOG.debug("Initialized {} gui plugins in {} ms",
						initializedPlugins.size(), System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			LOG.error("Failed to load gui plugins", e);
		}
	}

	public void unload() {
		unloadPlugins(initializedPlugins);
		initializedPlugins.clear();
		try {
			pluginsLoader.close();
		} catch (Exception e) {
			LOG.warn("Failed to close gui plugins loader", e);
		}
	}

	static List<JadxGuiPlugin> initPlugins(List<JadxPlugin> plugins, Set<String> disabledPlugins,
			VerifyRequiredVersion verifyRequiredVersion, Function<JadxPluginInfo, JadxGuiContext> contextBuilder) {
		List<JadxGuiPlugin> initialized = new ArrayList<>(plugins.size());
		for (JadxPlugin plugin : plugins) {
			if (!(plugin instanceof JadxGuiPlugin)) {
				continue;
			}
			JadxGuiPlugin guiPlugin = (JadxGuiPlugin) plugin;
			JadxPluginInfo pluginInfo = plugin.getPluginInfo();
			if (disabledPlugins.contains(pluginInfo.getPluginId())) {
				continue;
			}
			String requiredJadxVersion = pluginInfo.getRequiredJadxVersion();
			if (!verifyRequiredVersion.isCompatible(requiredJadxVersion)) {
				LOG.warn("Plugin '{}' not loaded: requires '{}' jadx version which it is not compatible with current: {}",
						pluginInfo.getPluginId(), requiredJadxVersion, verifyRequiredVersion.getJadxVersion());
				continue;
			}
			try {
				PluginContext.classLoaderWrap(guiPlugin.getClass().getClassLoader(),
						() -> guiPlugin.initGui(contextBuilder.apply(pluginInfo)));
				initialized.add(guiPlugin);
			} catch (Exception e) {
				LOG.error("Failed to init gui plugin: {}", pluginInfo.getPluginId(), e);
			}
		}
		return initialized;
	}

	static void unloadPlugins(List<JadxGuiPlugin> plugins) {
		for (JadxGuiPlugin plugin : plugins) {
			try {
				PluginContext.classLoaderWrap(plugin.getClass().getClassLoader(), plugin::unload);
			} catch (Exception e) {
				LOG.warn("Failed to unload gui plugin: {}", plugin.getPluginInfo().getPluginId(), e);
			}
		}
	}
}
