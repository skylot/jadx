package jadx.gui.ui.tab;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.panel.ContentPanel;

public class EditorSyncManager implements ITabStatesListener {
	private final MainWindow mainWindow;
	private final TabbedPane tabbedPane;

	public EditorSyncManager(MainWindow mainWindow, TabbedPane tabbedPane) {
		this.mainWindow = mainWindow;
		this.tabbedPane = tabbedPane;
		mainWindow.getTabsController().addListener(this);
	}

	public void sync() {
		ContentPanel selectedContentPanel = tabbedPane.getSelectedContentPanel();
		if (selectedContentPanel != null) {
			mainWindow.selectNodeInTree(selectedContentPanel.getNode());
		}
	}

	@Override
	public void onTabSelect(TabBlueprint blueprint) {
		if (mainWindow.getSettings().isAlwaysSelectOpened()) {
			// verify that tab opened for this blueprint (some nodes don't open tab with content)
			ContentPanel selectedContentPanel = tabbedPane.getSelectedContentPanel();
			if (selectedContentPanel != null && selectedContentPanel.getNode().equals(blueprint.getNode())) {
				sync();
			}
		}
	}
}
