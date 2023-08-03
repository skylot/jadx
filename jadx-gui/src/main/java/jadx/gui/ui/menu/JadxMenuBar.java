package jadx.gui.ui.menu;

import javax.swing.JMenu;
import javax.swing.JMenuBar;

public class JadxMenuBar extends JMenuBar {
	public void reloadShortcuts() {
		for (int i = 0; i < getMenuCount(); i++) {
			JMenu menu = getMenu(i);
			if (menu instanceof JadxMenu) {
				((JadxMenu) menu).reloadShortcuts();
			}
		}
	}
}
