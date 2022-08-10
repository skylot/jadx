package jadx.gui.plugins.context;

import javax.swing.JMenu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.gui.JadxGuiContext;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.ActionHandler;

public class PluginsContext implements JadxGuiContext {
	private static final Logger LOG = LoggerFactory.getLogger(PluginsContext.class);

	private final MainWindow mainWindow;

	public PluginsContext(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void reset() {
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.removeAll();
		pluginsMenu.setVisible(false);
	}

	@Override
	public void uiRun(Runnable runnable) {
		UiUtils.uiRun(runnable);
	}

	@Override
	public void addMenuAction(String name, Runnable action) {
		ActionHandler item = new ActionHandler(ev -> {
			try {
				mainWindow.getBackgroundExecutor().execute(name, action);
			} catch (Exception e) {
				LOG.error("Error running action for menu item: {}", name, e);
			}
		});
		item.setNameAndDesc(name);
		JMenu pluginsMenu = mainWindow.getPluginsMenu();
		pluginsMenu.add(item);
		pluginsMenu.setVisible(true);
	}
}
