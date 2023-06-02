package jadx.gui.settings.ui;

import javax.swing.tree.DefaultMutableTreeNode;

import jadx.api.plugins.gui.ISettingsGroup;

public class SettingsTreeNode extends DefaultMutableTreeNode {
	private final ISettingsGroup group;

	public SettingsTreeNode(ISettingsGroup group) {
		this.group = group;
	}

	public ISettingsGroup getGroup() {
		return group;
	}

	@Override
	public String toString() {
		return group.getTitle();
	}
}
