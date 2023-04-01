package jadx.gui.utils.plugins;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginContext;
import jadx.gui.JadxWrapper;

/**
 * Collect options from all plugins.
 * Init not yet loaded plugins in new temporary context.
 * Support case if decompiler in wrapper not initialized yet.
 */
public class CollectPluginOptions {

	private final JadxWrapper wrapper;

	public CollectPluginOptions(JadxWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public List<PluginContext> build() {
		SortedSet<PluginContext> allPlugins = new TreeSet<>();
		wrapper.getCurrentDecompiler()
				.ifPresent(decompiler -> allPlugins.addAll(decompiler.getPluginManager().getResolvedPluginContexts()));

		// collect and init not loaded plugins in new context
		try (JadxDecompiler decompiler = new JadxDecompiler(new JadxArgs())) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.load();
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
