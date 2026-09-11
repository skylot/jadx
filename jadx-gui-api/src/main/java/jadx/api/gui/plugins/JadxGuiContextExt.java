package jadx.api.gui.plugins;

import jadx.api.gui.IMainWindow;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.options.JadxPluginOptions;

/**
 * Jadx-gui extended API
 */
public interface JadxGuiContextExt extends JadxGuiContext {

	/**
	 * Access to all UI related objects and services
	 */
	IMainWindow getMainWindow();

	/**
	 * Allow to register options from global scope plugins
	 */
	void registerOptions(JadxPluginOptions options);
}
