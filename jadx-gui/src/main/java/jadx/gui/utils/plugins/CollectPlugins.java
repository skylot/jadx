package jadx.gui.utils.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.ui.MainWindow;
import jadx.plugins.tools.JadxExternalPluginsLoader;

/**
 * Collect all plugins.
 * Init not yet loaded plugins in new temporary context.
 * Support case if decompiler in wrapper not initialized yet.
 */
public class CollectPlugins {

	private final MainWindow mainWindow;

	public CollectPlugins(MainWindow mainWindow) {
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
		return new ArrayList<>(allPlugins);
	}
}
