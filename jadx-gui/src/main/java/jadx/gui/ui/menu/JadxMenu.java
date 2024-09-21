package jadx.gui.ui.menu;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.utils.shortcut.Shortcut;
import jadx.gui.utils.shortcut.ShortcutsController;

public class JadxMenu extends JMenu {
	// fake component to fill action shortcut component property
	private static final JComponent JADX_MENU_COMPONENT = new JComponent() {
		@Override
		public String toString() {
			return "JADX_MENU_COMPONENT";
		}
	};

	private final ShortcutsController shortcutsController;

	public JadxMenu(String name, ShortcutsController shortcutsController) {
		super(name);
		this.shortcutsController = shortcutsController;
	}

	@Override
	public JMenuItem add(JMenuItem menuItem) {
		Action action = menuItem.getAction();
		bindAction(action);
		return super.add(menuItem);
	}

	@Override
	public JMenuItem add(Action action) {
		bindAction(action);
		return super.add(action);
	}

	public void bindAction(Action action) {
		if (action instanceof JadxGuiAction) {
			JadxGuiAction guiAction = (JadxGuiAction) action;
			JComponent shortcutComponent = guiAction.getShortcutComponent();
			if (shortcutComponent == null) {
				guiAction.setShortcutComponent(JADX_MENU_COMPONENT);
			}
			shortcutsController.bind(guiAction);
		}
	}

	public void reloadShortcuts() {
		for (int i = 0; i < getItemCount(); i++) {
			// TODO only repaint the items whose shortcut changed
			JMenuItem item = getItem(i);
			if (item == null) {
				continue;
			}

			Action action = item.getAction();
			if (!(action instanceof JadxGuiAction) || ((JadxGuiAction) action).getActionModel() == null) {
				continue;
			}

			ActionModel actionModel = ((JadxGuiAction) action).getActionModel();
			Shortcut shortcut = shortcutsController.get(actionModel);
			if (shortcut != null) {
				item.setAccelerator(shortcut.toKeyStroke());
			} else {
				item.setAccelerator(null);
			}
			item.repaint();
			item.revalidate();
		}
	}
}
