package jadx.api.gui.plugins;

import java.util.function.Supplier;

import javax.swing.JComponent;

import jadx.api.gui.IMainWindow;
import jadx.api.gui.dock.DockableOptions;
import jadx.api.gui.dock.JadxDockable;
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

	/**
	 * Add dockable panel to main window.
	 * Panel position is saved between runs.
	 *
	 * @param content panel content, created on first show
	 */
	JadxDockable addDockable(DockableOptions options, Supplier<JComponent> content);
}
