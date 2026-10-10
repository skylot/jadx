package jadx.gui.plugins.context;

import java.lang.reflect.Field;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;

import jadx.gui.JadxWrapper;
import jadx.gui.events.types.JadxGuiEventsImpl;
import jadx.gui.plugins.GuiPluginsManager;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.shortcut.ShortcutsController;

public class TestMainWindowShim {

	public static @NotNull MainWindow build() {
		return build(new JMenu("Plugins"));
	}

	public static @NotNull MainWindow build(JMenu pluginsMenu) {
		NLS.setLocale(NLS.defaultLocale());
		try {
			Class<?> unsafeCls = Class.forName("sun.misc.Unsafe");
			Field theUnsafe = unsafeCls.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Object mainWindow = unsafeCls.getMethod("allocateInstance", Class.class).invoke(unsafe, MainWindow.class);
			Field menuField = MainWindow.class.getDeclaredField("pluginsMenu");
			menuField.setAccessible(true);
			menuField.set(mainWindow, pluginsMenu);

			JadxSettings settings = buildSettings();
			Field settingsField = MainWindow.class.getDeclaredField("settings");
			settingsField.setAccessible(true);
			settingsField.set(mainWindow, settings);

			ShortcutsController shortcutsController = new ShortcutsController(settings);
			shortcutsController.loadSettings();
			Field shortcutsField = MainWindow.class.getDeclaredField("shortcutsController");
			shortcutsField.setAccessible(true);
			shortcutsField.set(mainWindow, shortcutsController);

			Field eventsField = MainWindow.class.getDeclaredField("events");
			eventsField.setAccessible(true);
			eventsField.set(mainWindow, new JadxGuiEventsImpl());

			// JFrame field not accessible by reflection
			Field rootPaneField = JFrame.class.getDeclaredField("rootPane");
			Object rootPaneOffset = unsafeCls.getMethod("objectFieldOffset", Field.class).invoke(unsafe, rootPaneField);
			unsafeCls.getMethod("putObject", Object.class, long.class, Object.class)
					.invoke(unsafe, mainWindow, rootPaneOffset, new JRootPane());

			return (MainWindow) mainWindow;
		} catch (Throwable e) {
			Assumptions.abort("Can't build MainWindow instance for test: " + e);
			return null;
		}
	}

	public static void setPluginsManager(MainWindow mainWindow, GuiPluginsManager pluginsManager) {
		try {
			Field managerField = MainWindow.class.getDeclaredField("guiPluginsManager");
			managerField.setAccessible(true);
			managerField.set(mainWindow, pluginsManager);

			Field wrapperField = MainWindow.class.getDeclaredField("wrapper");
			wrapperField.setAccessible(true);
			wrapperField.set(mainWindow, new JadxWrapper(mainWindow));
		} catch (Throwable e) {
			Assumptions.abort("Can't set plugins manager for test: " + e);
		}
	}

	public static void waitForUiThread() {
		try {
			SwingUtilities.invokeAndWait(() -> {
			});
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	static JadxSettings buildSettings() {
		JadxSettings settings = new JadxSettings(JadxSettings.buildConfigAdapter());
		settings.loadSettingsFromJsonString("{}");
		return settings;
	}
}
