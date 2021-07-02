package jadx.gui.ui;

import javax.swing.*;

import org.jetbrains.annotations.Nullable;

import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;

public abstract class ContentPanel extends JPanel {

	private static final long serialVersionUID = 3237031760631677822L;

	protected final TabbedPane tabbedPane;
	protected final JNode node;

	protected ContentPanel(TabbedPane panel, JNode jnode) {
		tabbedPane = panel;
		node = jnode;
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
	 *
	 * If <code>null</code> is returned no tool tip will be displayed.
	 */
	@Nullable
	public String getTabTooltip() {
		JClass jClass = node.getRootClass();
		if (jClass != null) {
			return jClass.getFullName();
		}
		return node.getName();
	}
}
