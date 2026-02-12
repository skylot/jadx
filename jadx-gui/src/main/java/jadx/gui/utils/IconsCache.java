package jadx.gui.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.ImageIcon;

public class IconsCache {

	private static final Map<String, ImageIcon> SVG_ICONS = new ConcurrentHashMap<>();

	public static ImageIcon getSVGIcon(String name) {
		return SVG_ICONS.computeIfAbsent(name, UiUtils::openSvgIcon);
	}
}
