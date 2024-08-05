package jadx.gui.ui.panel;

import javax.swing.JPanel;

import org.jetbrains.annotations.Nullable;

import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.ui.tab.TabbedPane;

public abstract class ContentPanel extends JPanel {

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

	public JNode getNode() {
		return node;
	}

	/**
	 * Allows to show a tool tip on the tab e.g. for displaying a long path of the
	 * selected entry inside the APK file.
	 * <p>
	 * If <code>null</code> is returned no tool tip will be displayed.
	 */
	@Nullable
	public String getTabTooltip() {
		JClass jClass = getNode().getRootClass();
		if (jClass != null) {
			return jClass.getFullName();
		}
		return getNode().getName();
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
