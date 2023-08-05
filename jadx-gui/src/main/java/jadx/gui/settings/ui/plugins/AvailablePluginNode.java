package jadx.gui.settings.ui.plugins;

import jadx.plugins.tools.data.JadxPluginMetadata;

public class AvailablePluginNode extends BasePluginListNode {

	private final JadxPluginMetadata metadata;

	public AvailablePluginNode(JadxPluginMetadata metadata) {
		this.metadata = metadata;
	}

	@Override
	public String getTitle() {
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
	public String getLocationId() {
		return metadata.getLocationId();
	}

	@Override
	public PluginAction getAction() {
		return PluginAction.INSTALL;
	}
}
