package jadx.api.gui;

import javax.swing.JComponent;

import jadx.api.gui.tree.IJadxTree;

/**
 * Main access point to all Jadx-GUI objects and services
 */
public interface IMainWindow {

	IJadxSettings getSettings();

	IJadxTree getTree();

	/**
	 * Add zoom action listener and change font accordingly.
	 * Before use set font from jadx settings into a component.
	 * On font change 'update' method will be called.
	 */
	void registerZoomActions(JComponent component, Runnable update);
}
