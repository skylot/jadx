package jadx.gui.utils;

import java.awt.Component;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

public class OverlayIcon implements Icon {

	private final Icon icon;
	private final List<Icon> icons = new ArrayList<>(4);

	private static final double A = 0.8;
	private static final double B = 0.2;
	private static final double[] OVERLAY_POS = new double[] { A, B, B, B, A, A, B, A };

	public OverlayIcon(Icon icon) {
		this.icon = icon;
	}

	public OverlayIcon(Icon icon, Icon... ovrIcons) {
		this.icon = icon;
		Collections.addAll(icons, ovrIcons);
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
		for (Icon subIcon : icons) {
			int dx = (int) (OVERLAY_POS[k++] * (w - subIcon.getIconWidth()));
			int dy = (int) (OVERLAY_POS[k++] * (h - subIcon.getIconHeight()));
			subIcon.paintIcon(c, g, x + dx, y + dy);
		}
	}

	public void add(Icon icon) {
		icons.add(icon);
	}

	public List<Icon> getIcons() {
		return icons;
	}
}
