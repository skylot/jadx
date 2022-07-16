package jadx.gui.utils.ui;

import javax.swing.JMenuItem;

public class SimpleMenuItem extends JMenuItem {

	public SimpleMenuItem(String text, Runnable action) {
		super(text);
		addActionListener(ev -> action.run());
	}
}
