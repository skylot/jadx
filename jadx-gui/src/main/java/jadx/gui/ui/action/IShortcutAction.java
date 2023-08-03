package jadx.gui.ui.action;

import javax.swing.JComponent;

import jadx.gui.utils.shortcut.Shortcut;

public interface IShortcutAction {
	ActionModel getActionModel();

	JComponent getShortcutComponent();

	void performAction();

	void setShortcut(Shortcut shortcut);
}
