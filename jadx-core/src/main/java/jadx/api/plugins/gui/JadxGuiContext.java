package jadx.api.plugins.gui;

public interface JadxGuiContext {

	/**
	 * Run code in UI Thread
	 */
	void uiRun(Runnable runnable);

	void addMenuAction(String name, Runnable action);
}
