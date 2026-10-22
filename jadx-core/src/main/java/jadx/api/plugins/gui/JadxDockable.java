package jadx.api.plugins.gui;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import org.jetbrains.annotations.Nullable;

/**
 * Plugin dockable panel, see {@link JadxGuiContext#addDockable}.
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
