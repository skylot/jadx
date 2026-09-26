package jadx.gui.plugins;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.core.plugins.files.IJadxFilesGetter;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

public class GuiPluginsManager {
	private static final Logger LOG = LoggerFactory.getLogger(GuiPluginsManager.class);

	private final MainWindow mainWindow;
	private final CommonGuiPluginsContext guiPluginsContext;

	private @Nullable JadxPluginManager globalPluginManager;

	public GuiPluginsManager(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
		this.guiPluginsContext = new CommonGuiPluginsContext(mainWindow);
	}

	public void load() {
		try {
			JadxArgs jadxArgs = mainWindow.getSettings().toJadxArgs();
			jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
			loadGlobalPlugins(jadxArgs, new JadxExternalPluginsLoader(JadxGlobalGuiPlugin.class::isAssignableFrom));
		} catch (Exception e) {
			LOG.error("Failed to load gui plugins", e);
		}
	}

	void loadGlobalPlugins(JadxArgs jadxArgs, JadxPluginLoader pluginLoader) {
		long start = System.currentTimeMillis();
		JadxPluginManager pluginManager = new JadxPluginManager(jadxArgs);
		initGuiPluginsContext(pluginManager, jadxArgs.getFilesGetter(), true);
		globalPluginManager = pluginManager;
		pluginManager.load(pluginLoader);
		SortedSet<PluginContext> globalPlugins = pluginManager.getResolvedPluginContexts();
		runGlobalInit(globalPlugins);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Initialized {} global gui plugins in {} ms",
					globalPlugins.size(), System.currentTimeMillis() - start);
		}
	}

	public JadxPluginLoader buildProjectPluginLoader() {
		return new JadxExternalPluginsLoader(cls -> !JadxGlobalGuiPlugin.class.isAssignableFrom(cls));
	}

	public void initGuiPluginsContext(JadxDecompiler decompiler) {
		initGuiPluginsContext(decompiler.getPluginManager(), decompiler.getArgs().getFilesGetter(), false);
	}

	private void initGuiPluginsContext(JadxPluginManager pluginManager, IJadxFilesGetter filesGetter, boolean isGlobalPlugin) {
		pluginManager.registerAddPluginListener(pluginContext -> {
			AppContext appContext = new AppContext();
			appContext.setGuiContext(guiPluginsContext.buildForPlugin(pluginContext, isGlobalPlugin));
			appContext.setFilesGetter(filesGetter);
			pluginContext.setAppContext(appContext);
		});
	}

	/**
	 * Inject global plugin into project decompiler and transfer plugin context data
	 */
	public void injectGlobalPlugins(JadxDecompiler decompiler) {
		for (PluginContext globalContext : getGlobalPluginContexts()) {
			PluginContext projectContext = decompiler.getPluginManager().register(globalContext.getPluginInstance());
			if (projectContext != null) {
				// copy options and gui data
				projectContext.registerOptions(globalContext.getOptions());
				guiPluginsContext.copyGlobalPluginData(globalContext, projectContext);
			} else {
				LOG.warn("Failed to register plugin in project decompiler: {}", globalContext.getPluginId());
			}
		}
	}

	public SortedSet<PluginContext> getGlobalPluginContexts() {
		if (globalPluginManager == null) {
			// global plugins load failed or not finished yet
			return Collections.emptySortedSet();
		}
		return globalPluginManager.getResolvedPluginContexts();
	}

	public void resetProjectScope() {
		guiPluginsContext.resetProjectScope();
	}

	private void runGlobalInit(SortedSet<PluginContext> globalPlugins) {
		for (PluginContext pluginContext : globalPlugins) {
			JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginContext.getPluginInstance();
			try {
				PluginContext.classLoaderWrap(plugin.getClass().getClassLoader(), () -> {
					JadxGuiContext guiContext = Objects.requireNonNull(pluginContext.getGuiContext());
					plugin.globalInit((JadxGuiContextExt) guiContext);
				});
			} catch (Exception e) {
				LOG.warn("Failed to init global gui plugin: {}", pluginContext.getPluginId(), e);
			}
		}
	}

	public synchronized void runGlobalUnload() {
		try {
			for (PluginContext pluginContext : getGlobalPluginContexts()) {
				try {
					JadxGlobalGuiPlugin plugin = (JadxGlobalGuiPlugin) pluginContext.getPluginInstance();
					PluginContext.classLoaderWrap(plugin.getClass().getClassLoader(), plugin::globalUnload);
				} catch (Exception e) {
					LOG.warn("Failed to unload global gui plugin: {}", pluginContext.getPluginId(), e);
				}
			}
		} catch (Exception e) {
			LOG.warn("Failed to unload global gui plugins", e);
		}
	}

	public CommonGuiPluginsContext getPluginsContext() {
		return guiPluginsContext;
	}
}
