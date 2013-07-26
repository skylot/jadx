package jadx.gui.utils;

import javax.swing.Icon;
import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OverlayIcon implements Icon {

	private final Icon icon;
	private final List<Icon> icons = new ArrayList<Icon>(4);

	private static final double A = 0.8;
	private static final double B = 0.2;
	private static final double[] pos = new double[]{A, B, B, B, A, A, B, A};

	public OverlayIcon(Icon icon) {
		this.icon = icon;
	}

	@Override
	public int getIconHeight() {
		return icon.getIconHeight();
	}

	@Override
	public int getIconWidth() {
		return icon.getIconWidth();
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		int w = getIconWidth();
		int h = getIconHeight();

		icon.paintIcon(c, g, x, y);
		int k = 0;
		for (Icon icon : icons) {
			int dx = (int) (pos[k++] * (w - icon.getIconWidth()));
			int dy = (int) (pos[k++] * (h - icon.getIconHeight()));
			icon.paintIcon(c, g, x + dx, y + dy);
		}
	}

	public void add(Icon icon) {
		icons.add(icon);
	}

	public void addAll(Collection<Icon> icons) {
		icons.addAll(icons);
	}

	public List<Icon> getIcons() {
		return icons;
	}
}
