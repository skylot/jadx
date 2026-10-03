package jadx.gui.utils.plugins;

import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.ui.MainWindow;

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

	public List<PluginRuntime> build() {
		Optional<JadxDecompiler> currentDecompiler = mainWindow.getWrapper().getCurrentDecompiler();
		if (currentDecompiler.isPresent()) {
			JadxDecompiler decompiler = currentDecompiler.get();
			return decompiler.getPluginManager().getResolvedPlugins()
					.stream()
					.filter(PluginRuntime::isInitialized)
					.collect(Collectors.toList());
		}
		SortedSet<PluginRuntime> globalPlugins = mainWindow.getGuiPluginsManager().getGlobalPlugins();
		// collect and init plugins in new temp context
		JadxArgs jadxArgs = mainWindow.getSettings().toJadxArgs();
		jadxArgs.setFilesGetter(JadxFilesGetter.INSTANCE);
		// decompiler used only for plugins init, don't close it to keep shared temp files
		JadxDecompiler decompiler = new JadxDecompiler(jadxArgs);
		JadxPluginManager pluginManager = decompiler.getPluginManager();
		pluginManager.registerAddPluginListener(pluginContext -> {
			AppContext appContext = new AppContext();
			appContext.setGuiContext(null); // load temp plugins without UI context
			appContext.setFilesGetter(jadxArgs.getFilesGetter());
			pluginContext.setAppContext(appContext);
		});
		pluginManager.load(mainWindow.getGuiPluginsManager().buildProjectPluginLoader());
		SortedSet<PluginRuntime> allPlugins = pluginManager.getAllPlugins();
		pluginManager.init(decompiler, allPlugins);
		// global plugins don't have plugin context without opened project
		List<PluginRuntime> plugins = Stream.concat(
				globalPlugins.stream(),
				allPlugins.stream().filter(PluginRuntime::isInitialized))
				.collect(Collectors.toList());
		pluginManager.unload(allPlugins);
		return plugins;
	}
}
