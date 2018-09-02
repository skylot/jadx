package jadx.gui.utils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.InputStream;
import java.net.URL;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class Utils {
	private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

	private static final ImageIcon ICON_STATIC = openIcon("static_co");
	private static final ImageIcon ICON_FINAL = openIcon("final_co");
	private static final ImageIcon ICON_ABSTRACT = openIcon("abstract_co");
	private static final ImageIcon ICON_NATIVE = openIcon("native_co");

	public static final Font FONT_HACK = openFontTTF("Hack-Regular");

	private Utils() {
	}

	public static ImageIcon openIcon(String name) {
		String iconPath = "/icons-16/" + name + ".png";
		URL resource = Utils.class.getResource(iconPath);
		if (resource == null) {
			throw new JadxRuntimeException("Icon not found: " + iconPath);
		}
		return new ImageIcon(resource);
	}

	@Nullable
	public static Font openFontTTF(String name) {
		String fontPath = "/fonts/" + name + ".ttf";
		try (InputStream is = Utils.class.getResourceAsStream(fontPath)) {
			Font font = Font.createFont(Font.TRUETYPE_FONT, is);
			return font.deriveFont(12f);
		} catch (Exception e) {
			LOG.error("Failed load font by path: {}", fontPath, e);
			return null;
		}
	}

	public static void addKeyBinding(JComponent comp, KeyStroke key, String id, Action action) {
		comp.getInputMap().put(key, id);
		comp.getActionMap().put(id, action);
	}

	public static String typeFormat(String name, ArgType type) {
		return "<html><body><nobr>" + name
				+ "<span style='color:#888888;'> : " + typeStr(type) + "</span>"
				+ "</nobr></body></html>";
	}

	public static String typeStr(ArgType type) {
		if (type == null) {
			return "null";
		}
		if (type.isObject()) {
			String cls = type.getObject();
			int dot = cls.lastIndexOf('.');
			if (dot != -1) {
				return cls.substring(dot + 1);
			} else {
				return cls;
			}
		}
		if (type.isArray()) {
			return typeStr(type.getArrayElement()) + "[]";
		}
		return type.toString();
	}

	public static OverlayIcon makeIcon(AccessInfo af, Icon pub, Icon pri, Icon pro, Icon def) {
		Icon icon;
		if (af.isPublic()) {
			icon = pub;
		} else if (af.isPrivate()) {
			icon = pri;
		} else if (af.isProtected()) {
			icon = pro;
		} else {
			icon = def;
		}
		OverlayIcon overIcon = new OverlayIcon(icon);
		if (af.isFinal()) {
			overIcon.add(ICON_FINAL);
		}
		if (af.isStatic()) {
			overIcon.add(ICON_STATIC);
		}
		if (af.isAbstract()) {
			overIcon.add(ICON_ABSTRACT);
		}
		if (af.isNative()) {
			overIcon.add(ICON_NATIVE);
		}
		return overIcon;
	}

	public static boolean isFreeMemoryAvailable() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalFree = runtime.freeMemory() + maxMemory - runtime.totalMemory();
		return totalFree > maxMemory * 0.2;
	}

	public static String memoryInfo() {
		Runtime runtime = Runtime.getRuntime();
		StringBuilder sb = new StringBuilder();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		sb.append("heap: ").append(format(allocatedMemory - freeMemory));
		sb.append(", allocated: ").append(format(allocatedMemory));
		sb.append(", free: ").append(format(freeMemory));
		sb.append(", total free: ").append(format(freeMemory + maxMemory - allocatedMemory));
		sb.append(", max: ").append(format(maxMemory));

		return sb.toString();
	}

	private static String format(long mem) {
		return Long.toString((long) (mem / 1024. / 1024.)) + "MB";
	}

	/**
	 * Adapt character case for case insensitive searches
	 */
	public static char caseChar(char ch, boolean toLower) {
		return toLower ? Character.toLowerCase(ch) : ch;
	}

	public static void setClipboardString(String text) {
		try {
			Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
			Transferable transferable = new StringSelection(text);
			clipboard.setContents(transferable, null);
			LOG.debug("String '{}' copied to clipboard", text);
		} catch (Exception e) {
			LOG.error("Failed copy string '{}' to clipboard", text, e);
		}
	}
}
