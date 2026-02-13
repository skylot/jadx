package jadx.gui.ui.codearea;

import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuListener;

import jadx.gui.ui.action.JNodeAction;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.utils.shortcut.ShortcutsController;

public class JNodePopupBuilder {
	private final JPopupMenu menu;
	private final JNodePopupListener popupListener;
	private final ShortcutsController shortcutsController;

	public JNodePopupBuilder(CodeArea codeArea, JPopupMenu popupMenu, ShortcutsController shortcutsController) {
		this.shortcutsController = shortcutsController;
		menu = popupMenu;
		popupListener = new JNodePopupListener(codeArea);
		popupMenu.addPopupMenuListener(popupListener);
	}

	public void addSeparator() {
		menu.addSeparator();
	}

	public void add(JNodeAction nodeAction) {
		// We set the shortcut immediately for two reasons
		// - there might be multiple instances of this action with
		// same ActionModel across different codeAreas, while
		// ShortcutController only supports one instance
		// - This action will be recreated when shortcuts are changed,
		// so no need to bind it
		if (nodeAction.getActionModel() != null) {
			shortcutsController.bindImmediate(nodeAction);
		}
		menu.add(nodeAction);
		popupListener.addActions(nodeAction);
	}

	public void addSubmenu(JNodeAction[] nodeActions, String name) {
		JMenu submenu = new JMenu(name);

		for (JNodeAction nodeAction : nodeActions) {
			if (nodeAction.getActionModel() != null) {
				shortcutsController.bindImmediate(nodeAction);
			}

			submenu.add(nodeAction);
			popupListener.addActions(nodeAction);
		}

		menu.add(submenu);
	}

	public void add(JadxGuiAction action) {
		if (action.getActionModel() != null) {
			shortcutsController.bindImmediate(action);
		}
		menu.add(action);
		if (action instanceof PopupMenuListener) {
			menu.addPopupMenuListener((PopupMenuListener) action);
		}
	}

	public JPopupMenu getMenu() {
		return menu;
	}
}
