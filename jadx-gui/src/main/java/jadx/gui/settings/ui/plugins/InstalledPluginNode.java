package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.core.plugins.PluginContext;
import jadx.plugins.tools.data.JadxPluginMetadata;

public class InstalledPluginNode extends BasePluginListNode {
	private final PluginContext plugin;
	private final JadxPluginMetadata metadata;

	public InstalledPluginNode(PluginContext plugin, @Nullable JadxPluginMetadata metadata) {
		this.plugin = plugin;
		this.metadata = metadata;
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
	public PluginAction getAction() {
		if (metadata != null) {
			return PluginAction.UNINSTALL;
		}
		return PluginAction.NONE;
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
