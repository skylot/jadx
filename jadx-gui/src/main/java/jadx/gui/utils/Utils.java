package jadx.gui.utils;

import jadx.core.utils.exceptions.JadxRuntimeException;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
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

	public static void addKeyBinding(JComponent comp, KeyStroke key, String id, AbstractAction action) {
		comp.getInputMap().put(key, id);
		comp.getActionMap().put(id, action);
	}
}
