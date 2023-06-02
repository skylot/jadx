package jadx.gui.utils.plugins;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

/**
 * Collect options from all plugins.
 * Init not yet loaded plugins in new temporary context.
 * Support case if decompiler in wrapper not initialized yet.
 */
public class CollectPluginOptions {

	private final MainWindow mainWindow;

	public CollectPluginOptions(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public List<PluginContext> build() {
		SortedSet<PluginContext> allPlugins = new TreeSet<>();
		mainWindow.getWrapper().getCurrentDecompiler()
				.ifPresent(decompiler -> allPlugins.addAll(decompiler.getPluginManager().getResolvedPluginContexts()));

		// collect and init not loaded plugins in new context
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.load(new JadxExternalPluginsLoader());
			CommonGuiPluginsContext guiPluginsContext = new CommonGuiPluginsContext(mainWindow);
			decompiler.getPluginManager().registerAddPluginListener(pluginContext -> {
				GuiPluginContext guiContext = guiPluginsContext.buildForPlugin(pluginContext);
				pluginContext.setGuiContext(guiContext);
			});
			SortedSet<PluginContext> missingPlugins = new TreeSet<>();
			for (PluginContext context : pluginManager.getAllPluginContexts()) {
				if (!allPlugins.contains(context)) {
					missingPlugins.add(context);
				}
			}
			pluginManager.init(missingPlugins);
			allPlugins.addAll(missingPlugins);
		}
		return allPlugins.stream()
				.filter(context -> context.getOptions() != null)
				.sorted()
				.collect(Collectors.toList());
	}
}
