package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.JadxPluginInfo;
import jadx.core.plugins.PluginContext;
import jadx.plugins.tools.data.JadxPluginMetadata;

public class PluginListNode extends BasePluginListNode {
	private final PluginContext plugin;
	private final JadxPluginMetadata metadata;

	public PluginListNode(PluginContext plugin, @Nullable JadxPluginMetadata metadata) {
		this.plugin = plugin;
		this.metadata = metadata;
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return plugin.getPluginInfo();
	}

	public @Nullable JadxPluginMetadata getMetadata() {
		return metadata;
	}

	@Override
	public @Nullable String getVersion() {
		if (metadata != null) {
			return metadata.getVersion();
		}
		return null;
	}

	@Override
	public String toString() {
		return plugin.getPluginInfo().getName();
	}
}
