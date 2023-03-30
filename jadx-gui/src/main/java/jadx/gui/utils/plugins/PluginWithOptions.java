package jadx.gui.utils.plugins;

import org.jetbrains.annotations.NotNull;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.options.JadxPluginOptions;

public class PluginWithOptions implements Comparable<PluginWithOptions> {
	public static final PluginWithOptions NULL = new PluginWithOptions(null, null);

	private final JadxPlugin plugin;
	private final JadxPluginOptions options;

	public PluginWithOptions(JadxPlugin plugin, JadxPluginOptions options) {
		this.plugin = plugin;
		this.options = options;
	}

	public JadxPlugin getPlugin() {
		return plugin;
	}

	public JadxPluginOptions getOptions() {
		return options;
	}

	@Override
	public int compareTo(@NotNull PluginWithOptions other) {
		return plugin.getClass().getName().compareTo(other.getClass().getName());
	}

	@Override
	public String toString() {
		return "PluginWithOptions{plugin=" + plugin + ", options=" + options + '}';
	}
}
