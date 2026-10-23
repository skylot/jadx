package jadx.api.gui.dock;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jetbrains.annotations.Nullable;

import jadx.api.gui.plugins.JadxGuiContextExt;

/**
 * Plugin dockable panel, see {@link JadxGuiContextExt#addDockable}.
 * Methods should be called in UI thread.
 */
public interface JadxDockable {

	String getId();

	/**
	 * Show panel and bring it to front
	 */
	void show();

	void hide();

	boolean isShown();

	void setTitle(String title);

	void setIcon(@Nullable ImageIcon icon);

	/**
	 * Panel content, created on first access
	 */
	JComponent getContent();

	/**
	 * Remove panel from main window
	 */
	void remove();
}
