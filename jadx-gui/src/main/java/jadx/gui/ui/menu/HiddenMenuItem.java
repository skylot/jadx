package jadx.gui.ui.menu;

import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.Action;
import javax.swing.JMenuItem;

public class HiddenMenuItem extends JMenuItem {
	public HiddenMenuItem(Action a) {
		super(a);
	}

	@Override
	protected void paintComponent(Graphics g) {
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(0, 0);
	}
}
