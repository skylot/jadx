package jadx.gui;

import jadx.core.utils.exceptions.JadxRuntimeException;

import javax.swing.ImageIcon;
import java.net.URL;

public class Utils {

	public static ImageIcon openIcon(String name) {
		String iconPath = "/icons-16/" + name + ".png";
		URL resource = Utils.class.getResource(iconPath);
		if (resource == null) {
			throw new JadxRuntimeException("Icon not found: " + iconPath);
		}
		return new ImageIcon(resource);
	}
}
