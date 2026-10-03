package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.core.plugins.PluginRuntime;

public class LoadedPluginNode extends BasePluginListNode {
	private final PluginRuntime plugin;

	public LoadedPluginNode(PluginRuntime plugin) {
		this.plugin = plugin;
	}

	@Override
	public @Nullable String getTitle() {
		return plugin.getPluginInfo().getName();
	}

	@Override
	public boolean hasDetails() {
		return true;
	}

	@Override
	public String getPluginId() {
		return plugin.getPluginId();
	}

	@Override
	public String getDescription() {
		return plugin.getPluginInfo().getDescription();
	}

	@Override
	public String getHomepage() {
		return plugin.getPluginInfo().getHomepage();
	}

	@Override
	public String toString() {
		return plugin.getPluginInfo().getName();
	}
}
