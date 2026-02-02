package jadx.gui.ui.panel;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.ui.tab.TabsController;

public abstract class ContentPanel extends JPanel {
	private static final Logger LOG = LoggerFactory.getLogger(ContentPanel.class);
	private static final long serialVersionUID = 3237031760631677822L;

	protected TabbedPane tabbedPane;
	protected JNode node;

	protected ContentPanel(TabbedPane panel, JNode node) {
		tabbedPane = panel;
		this.node = node;
	}

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

	public JNode getNode() {
		return node;
	}

	public void scrollToPos(int pos) {
		LOG.warn("ContentPanel.scrollToPos method not implemented, class: {}", getClass().getSimpleName());
	}

	public JadxSettings getSettings() {
		return tabbedPane.getMainWindow().getSettings();
	}

	public boolean supportsQuickTabs() {
		return getNode().supportsQuickTabs();
	}

	public void dispose() {
		tabbedPane = null;
		node = null;
	}
}
