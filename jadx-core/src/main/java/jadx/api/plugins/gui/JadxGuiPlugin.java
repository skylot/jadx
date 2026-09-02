package jadx.api.plugins.gui;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;

public interface JadxGuiPlugin extends JadxPlugin {

	void initGui(JadxGuiContext context);

	@Override
	default void init(JadxPluginContext context) { // not used: application scoped plugins are not initialized in decompiler scope
	}
}
