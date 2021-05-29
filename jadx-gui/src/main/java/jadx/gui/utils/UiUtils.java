package jadx.gui.utils;

import java.awt.Component;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.intellij.lang.annotations.MagicConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.info.AccessInfo;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.ui.codearea.AbstractCodeArea;

public class UiUtils {
	private static final Logger LOG = LoggerFactory.getLogger(UiUtils.class);

	private static final ImageIcon ICON_STATIC = openIcon("static_co");
	private static final ImageIcon ICON_FINAL = openIcon("final_co");
	private static final ImageIcon ICON_ABSTRACT = openIcon("abstract_co");
	private static final ImageIcon ICON_NATIVE = openIcon("native_co");

	/**
	 * The minimum about of memory in bytes we are trying to keep free, otherwise the application may
	 * run out of heap
	 * which ends up in a Java garbage collector running "amok" (CPU utilization 100% for each core and
	 * the UI is
	 * not responsive).
	 * <p>
	 * We can calculate and store this value here as the maximum heap is fixed for each JVM instance
	 * and can't be changed at runtime.
	 */
	public static final long MIN_FREE_MEMORY = calculateMinFreeMemory();

	private UiUtils() {
	}

	public static ImageIcon openIcon(String name) {
		String iconPath = "/icons-16/" + name + ".png";
		URL resource = UiUtils.class.getResource(iconPath);
		if (resource == null) {
			throw new JadxRuntimeException("Icon not found: " + iconPath);
		}
		return new ImageIcon(resource);
	}

	public static Image openImage(String path) {
		URL resource = UiUtils.class.getResource(path);
		if (resource == null) {
			throw new JadxRuntimeException("Image not found: " + path);
		}
		return Toolkit.getDefaultToolkit().createImage(resource);
	}

	public static void addKeyBinding(JComponent comp, KeyStroke key, String id, Action action) {
		comp.getInputMap().put(key, id);
		comp.getActionMap().put(id, action);
	}

	public static void removeKeyBinding(JComponent comp, KeyStroke key, String id) {
		comp.getInputMap().remove(key);
		comp.getActionMap().remove(id);
	}

	public static String typeFormat(String name, ArgType type) {
		return name + " " + typeStr(type);
	}

	public static String typeFormatHtml(String name, ArgType type) {
		return "<html><body><nobr>" + escapeHtml(name)
				+ "<span style='color:#888888;'> " + escapeHtml(typeStr(type)) + "</span>"
				+ "</nobr></body></html>";
	}

	public static String escapeHtml(String str) {
		return str.replace("<", "&lt;");
	}

	public static String typeStr(ArgType type) {
		if (type == null) {
			return "null";
		}
		if (type.isObject()) {
			String cls = type.toString();
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

	/**
	 * @return 20% of the maximum heap size limited to 512 MB (bytes)
	 */
	public static long calculateMinFreeMemory() {
		Runtime runtime = Runtime.getRuntime();
		long minFree = (long) (runtime.maxMemory() * 0.2);
		return Math.min(minFree, 512 * 1024L * 1024L);
	}

	public static boolean isFreeMemoryAvailable() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long totalFree = runtime.freeMemory() + (maxMemory - runtime.totalMemory());
		return totalFree > MIN_FREE_MEMORY;
	}

	public static String memoryInfo() {
		Runtime runtime = Runtime.getRuntime();
		long maxMemory = runtime.maxMemory();
		long allocatedMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();

		return "heap: " + format(allocatedMemory - freeMemory)
				+ ", allocated: " + format(allocatedMemory)
				+ ", free: " + format(freeMemory)
				+ ", total free: " + format(freeMemory + maxMemory - allocatedMemory)
				+ ", max: " + format(maxMemory);
	}

	private static String format(long mem) {
		return (long) (mem / (double) (1024L * 1024L)) + "MB";
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

	public static void setWindowIcons(Window window) {
		List<Image> icons = new ArrayList<>();
		icons.add(UiUtils.openImage("/logos/jadx-logo-16px.png"));
		icons.add(UiUtils.openImage("/logos/jadx-logo-32px.png"));
		icons.add(UiUtils.openImage("/logos/jadx-logo-48px.png"));
		icons.add(UiUtils.openImage("/logos/jadx-logo.png"));
		window.setIconImages(icons);
	}

	public static final int CTRL_BNT_KEY = getCtrlButton();

	@SuppressWarnings("deprecation")
	@MagicConstant(flagsFromClass = InputEvent.class)
	private static int getCtrlButton() {
		if (SystemInfo.IS_MAC) {
			return Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
		} else {
			return InputEvent.CTRL_DOWN_MASK;
		}
	}

	@MagicConstant(flagsFromClass = InputEvent.class)
	public static int ctrlButton() {
		return CTRL_BNT_KEY;
	}

	public static void showMessageBox(Component parent, String msg) {
		JOptionPane.showMessageDialog(parent, msg);
	}

	public static void addEscapeShortCutToDispose(JDialog dialog) {
		KeyStroke stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), stroke, JComponent.WHEN_IN_FOCUSED_WINDOW);
	}

	/**
	 * Get closest offset at mouse position
	 *
	 * @return -1 on error
	 */
	@SuppressWarnings("deprecation")
	public static int getOffsetAtMousePosition(AbstractCodeArea codeArea) {
		try {
			Point mousePos = getMousePosition(codeArea);
			return codeArea.viewToModel(mousePos);
		} catch (Exception e) {
			LOG.error("Failed to get offset at mouse position", e);
			return -1;
		}
	}

	public static Point getMousePosition(Component comp) {
		Point pos = MouseInfo.getPointerInfo().getLocation();
		SwingUtilities.convertPointFromScreen(pos, comp);
		return pos;
	}

	public static String getEnvVar(String varName, String defValue) {
		String envVal = System.getenv(varName);
		if (envVal == null) {
			return defValue;
		}
		return envVal;
	}

	public static void errorMessage(Component parent, String message) {
		JOptionPane.showMessageDialog(parent, message,
				NLS.str("message.errorTitle"), JOptionPane.ERROR_MESSAGE);
	}
}
