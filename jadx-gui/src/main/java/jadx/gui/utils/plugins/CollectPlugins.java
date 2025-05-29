package jadx.gui.utils.plugins;

import java.util.ArrayList;
import java.util.SortedSet;
import java.util.TreeSet;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

/**
 * Collect all plugins.
 * Init not yet loaded plugins in new temporary context.
 * Support a case if decompiler in wrapper is not initialized yet.
 */
public class CollectPlugins {

	private final MainWindow mainWindow;

	public CollectPlugins(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public CloseablePlugins build() {
		SortedSet<PluginContext> allPlugins = new TreeSet<>();
		mainWindow.getWrapper().getCurrentDecompiler()
				.ifPresent(decompiler -> allPlugins.addAll(decompiler.getPluginManager().getResolvedPluginContexts()));

		// collect and init not loaded plugins in new temp context
		Runnable closeable = null;
		JadxArgs jadxArgs = mainWindow.getSettings().toJadxArgs();
		jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
		try (JadxDecompiler decompiler = new JadxDecompiler(jadxArgs)) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.registerAddPluginListener(pluginContext -> {
				AppContext appContext = new AppContext();
				appContext.setGuiContext(null); // load temp plugins without UI context
				appContext.setFilesGetter(jadxArgs.getFilesGetter());
				pluginContext.setAppContext(appContext);
			});
			pluginManager.load(new JadxExternalPluginsLoader());
			SortedSet<PluginContext> missingPlugins = new TreeSet<>();
			for (PluginContext context : pluginManager.getAllPluginContexts()) {
				if (!allPlugins.contains(context)) {
					missingPlugins.add(context);
				}
			}
			if (!missingPlugins.isEmpty()) {
				pluginManager.init(missingPlugins);
				allPlugins.addAll(missingPlugins);
				closeable = () -> pluginManager.unload(missingPlugins);
			}
		}
		return new CloseablePlugins(new ArrayList<>(allPlugins), closeable);
	}
}
