package jadx.gui.utils.plugins;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.plugins.AppContext;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
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

	public CloseablePlugins build() {
		Optional<JadxDecompiler> currentDecompiler = mainWindow.getWrapper().getCurrentDecompiler();
		if (currentDecompiler.isPresent()) {
			JadxDecompiler decompiler = currentDecompiler.get();
			List<PluginContext> plugins = decompiler.getPluginManager().getResolvedPlugins()
					.stream()
					.map(PluginRuntime::getPluginContext)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			return new CloseablePlugins(plugins, null);
		}
		SortedSet<PluginRuntime> globalPlugins = mainWindow.getGuiPluginsManager().getGlobalPlugins();
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
			SortedSet<PluginRuntime> allPlugins = pluginManager.getAllPlugins();
			pluginManager.init(decompiler, allPlugins);
			Runnable closeable = () -> pluginManager.unload(allPlugins);
			List<PluginContext> plugins = Stream.concat(globalPlugins.stream(), allPlugins.stream())
					.map(PluginRuntime::getPluginContext)
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			return new CloseablePlugins(plugins, closeable);
		}
	}
}
