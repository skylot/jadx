package jadx.gui.settings.ui.plugins;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.JadxPluginInfo;

abstract class BasePluginListNode {

	public @Nullable String getTitle() {
		return null;
	}

	public JadxPluginInfo getPluginInfo() {
		return null;
	}

	public @Nullable String getVersion() {
		return null;
	}
}
