package jadx.gui.ui;

import javax.swing.*;

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
}
