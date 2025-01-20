package jadx.gui.utils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.formdev.flatlaf.extras.FlatSVGIcon;

public class IconsCache {

	private static final Map<String, FlatSVGIcon> SVG_ICONS = new ConcurrentHashMap<>();

	public static FlatSVGIcon getSVGIcon(String name) {
		return SVG_ICONS.computeIfAbsent(name, UiUtils::openSvgIcon);
	}
}
