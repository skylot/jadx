package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.plugins.tools.data.JadxPluginMetadata;

public class InstalledPluginNode extends BasePluginListNode {
	private final JadxPluginMetadata metadata;

	public InstalledPluginNode(JadxPluginMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public @Nullable String getTitle() {
		return metadata.getName();
	}

	@Override
	public boolean hasDetails() {
		return true;
	}

	@Override
	public String getPluginId() {
		return metadata.getPluginId();
	}

	@Override
	public String getDescription() {
		return metadata.getDescription();
	}

	@Override
	public String getHomepage() {
		return metadata.getHomepage();
	}

	@Override
	public PluginAction getAction() {
		return PluginAction.UNINSTALL;
	}

	@Override
	public @Nullable String getVersion() {
		return metadata.getVersion();
	}

	@Override
	public boolean isDisabled() {
		return metadata.isDisabled();
	}

	@Override
	public String toString() {
		return metadata.getName();
	}
}
