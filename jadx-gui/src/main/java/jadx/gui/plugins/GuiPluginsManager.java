package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.events.types.ReloadSettingsWindow;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.cli.JadxAppCommon;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

public class GuiPluginsManager {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsManager.class);

	private final MainWindow mainWindow;
	private final JadxArgs globalArgs;
	private final JadxPluginManager globalPluginManager;
	private final CommonGuiPluginsContext guiPluginsContext;

	public GuiPluginsManager(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.guiPluginsContext = new CommonGuiPluginsContext(mainWindow);
		this.globalArgs = buildJadxArgs(mainWindow.getSettings());
		this.globalPluginManager = new JadxPluginManager(globalArgs);
	}

	private static JadxArgs buildJadxArgs(JadxSettings settings) {
		JadxArgs jadxArgs = settings.toJadxArgs();
		jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
		JadxAppCommon.applyEnvVars(jadxArgs);
		return jadxArgs;
	}

	public void load() {
		try {
			long start = System.currentTimeMillis();

			initGuiPluginsContextForGlobalScope();
			globalPluginManager.load(new JadxExternalPluginsLoader(JadxGlobalGuiPlugin.class::isAssignableFrom));
			SortedSet<PluginRuntime> globalPlugins = globalPluginManager.getResolvedPlugins();
			runGlobalInit(globalPlugins);
			if (!globalPlugins.isEmpty()) {
				// settings window can be opened before global plugins init
				mainWindow.events().send(ReloadSettingsWindow.INSTANCE);
			}

			if (LOG.isDebugEnabled()) {
				LOG.debug("Initialized {} global gui plugins in {} ms",
						globalPlugins.size(), System.currentTimeMillis() - start);
			}
		} catch (Exception e) {
			LOG.error("Failed to load gui plugins", e);
		}
	}

	public JadxPluginLoader buildProjectPluginLoader() {
		return new JadxExternalPluginsLoader(cls -> !JadxGlobalGuiPlugin.class.isAssignableFrom(cls));
	}

	public void initGuiPluginsContextForGlobalScope() {
		initGuiPluginsContext(globalPluginManager, globalArgs, true);
	}

	public void initGuiPluginsContext(JadxPluginManager pluginManager, JadxArgs jadxArgs, boolean isGlobalPlugin) {
		pluginManager.registerAddPluginListener(pluginRuntime -> {
			AppContext appContext = new AppContext();
			appContext.setGuiContext(guiPluginsContext.buildForPlugin(pluginRuntime, isGlobalPlugin));
			appContext.setFilesGetter(jadxArgs.getFilesGetter());
			pluginRuntime.setAppContext(appContext);
		});
	}

	/**
	 * Inject global plugin into project decompiler and transfer plugin context data
	 */
	public void injectGlobalPlugins(JadxDecompiler decompiler) {
		for (PluginRuntime globalPlugin : getGlobalPlugins()) {
			PluginRuntime projectPlugin = decompiler.getPluginManager().register(globalPlugin.getPluginInstance());
			if (projectPlugin != null) {
				// copy options and gui data
				projectPlugin.registerOptions(globalPlugin.getOptions());
				guiPluginsContext.copyGlobalPluginData(globalPlugin, projectPlugin);
			} else {
				LOG.warn("Failed to register plugin in project decompiler: {}", globalPlugin.getPluginId());
			}
		}
	}

	public SortedSet<PluginRuntime> getGlobalPlugins() {
		return globalPluginManager.getResolvedPlugins();
	}

	public void resetProjectScope() {
		guiPluginsContext.resetProjectScope();
	}

	void runGlobalInit(SortedSet<PluginRuntime> globalPlugins) {
		List<String> failedPlugins = new ArrayList<>();
		for (PluginRuntime pluginRuntime : globalPlugins) {
			try {
				JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginRuntime.getPluginInstance();
				PluginRuntime.classLoaderWrap(plugin.getClass().getClassLoader(), () -> {
					AppContext appContext = pluginRuntime.getAppContext();
					if (appContext != null) {
						JadxGuiContext guiContext = Objects.requireNonNull(appContext.getGuiContext());
						plugin.globalInit((JadxGuiContextExt) guiContext);
					}
				});
			} catch (Throwable e) {
				LOG.warn("Failed to init global gui plugin: {}", pluginRuntime.getPluginId(), e);
				failedPlugins.add(pluginRuntime.getPluginId());
			}
		}
		// don't inject failed plugins into projects
		failedPlugins.forEach(globalPluginManager::unload);
	}

	public synchronized void runGlobalUnload() {
		try {
			for (PluginRuntime pluginRuntime : getGlobalPlugins()) {
				try {
					JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginRuntime.getPluginInstance();
					PluginRuntime.classLoaderWrap(plugin.getClass().getClassLoader(), plugin::globalUnload);
				} catch (Exception e) {
					LOG.warn("Failed to unload global gui plugin: {}", pluginRuntime.getPluginId(), e);
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to unload global gui plugins", e);
		}
	}

	public CommonGuiPluginsContext getPluginsContext() {
		return guiPluginsContext;
	}

	public JadxPluginManager getGlobalPluginManager() {
		return globalPluginManager;
	}
}
