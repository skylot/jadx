package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

abstract class BasePluginListNode {

	public abstract String getTitle();

	public abstract boolean hasDetails();

	public String getPluginId() {
		return null;
	}

	public String getDescription() {
		return null;
	}

	public String getHomepage() {
		return null;
	}

	public @Nullable String getLocationId() {
		return null;
	}

	public @Nullable String getVersion() {
		return null;
	}

	public PluginAction getAction() {
		return PluginAction.NONE;
	}
}
