package jadx.gui.ui.panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.gui.code.IScrollableContentPanel;
import jadx.api.gui.code.JadxContentPanel;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.ui.tab.TabsController;

public abstract class ContentPanel extends JadxContentPanel implements IScrollableContentPanel {
	private static final Logger LOG = LoggerFactory.getLogger(ContentPanel.class);
	private static final long serialVersionUID = 3237031760631677822L;

	protected TabbedPane tabbedPane;

	protected ContentPanel(TabbedPane panel, JNode node) {
		super(node);
		tabbedPane = panel;
	}

	@Override
	public abstract void loadSettings();

	public TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public TabsController getTabsController() {
		return tabbedPane.getTabsController();
	}

	public MainWindow getMainWindow() {
		return tabbedPane.getMainWindow();
	}

	@Override
	public void scrollToPos(int pos) {
		LOG.warn("ContentPanel.scrollToPos method not implemented, class: {}", getClass().getSimpleName());
	}

	public JadxSettings getSettings() {
		return tabbedPane.getMainWindow().getSettings();
	}

	public boolean supportsQuickTabs() {
		return getNode().supportsQuickTabs();
	}

	@Override
	public void dispose() {
		tabbedPane = null;
	}
}
