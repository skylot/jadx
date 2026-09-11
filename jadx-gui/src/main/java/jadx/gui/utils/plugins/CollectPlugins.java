package jadx.gui.utils.plugins;

import java.util.ArrayList;
import java.util.Optional;
import java.util.SortedSet;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.gui.ui.MainWindow;

import static jadx.core.utils.ListUtils.concatSetsToList;

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
		Optional<JadxDecompiler> currentDecompiler = mainWindow.getWrapper().getCurrentDecompiler();
		if (currentDecompiler.isPresent()) {
			JadxDecompiler decompiler = currentDecompiler.get();
			SortedSet<PluginContext> plugins = decompiler.getPluginManager().getResolvedPluginContexts();
			return new CloseablePlugins(new ArrayList<>(plugins), null);
		}
		SortedSet<PluginContext> globalPlugins = mainWindow.getGuiPluginsManager().getGlobalPluginContexts();
		// collect and init plugins in new temp context
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
			pluginManager.load(mainWindow.getGuiPluginsManager().buildProjectPluginLoader());
			SortedSet<PluginContext> allPlugins = pluginManager.getAllPluginContexts();
			pluginManager.init(allPlugins);
			Runnable closeable = () -> pluginManager.unload(allPlugins);
			return new CloseablePlugins(concatSetsToList(allPlugins, globalPlugins), closeable);
		}
	}
}
