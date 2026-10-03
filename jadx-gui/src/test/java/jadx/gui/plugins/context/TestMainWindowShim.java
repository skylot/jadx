package jadx.gui.plugins.context;

import java.lang.reflect.Field;

import javax.swing.JMenu;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assumptions;

import jadx.gui.JadxWrapper;
import jadx.gui.events.types.JadxGuiEventsImpl;
import jadx.gui.plugins.GuiPluginsManager;
import jadx.gui.settings.JadxSettings;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class TestMainWindowShim {

	public static @NotNull MainWindow build() {
		NLS.setLocale(NLS.defaultLocale());
		try {
			Class<?> unsafeCls = Class.forName("sun.misc.Unsafe");
			Field theUnsafe = unsafeCls.getDeclaredField("theUnsafe");
			theUnsafe.setAccessible(true);
			Object unsafe = theUnsafe.get(null);
			Object mainWindow = unsafeCls.getMethod("allocateInstance", Class.class).invoke(unsafe, MainWindow.class);
			Field menuField = MainWindow.class.getDeclaredField("pluginsMenu");
			menuField.setAccessible(true);
			menuField.set(mainWindow, new JMenu("Plugins"));

			Field settingsField = MainWindow.class.getDeclaredField("settings");
			settingsField.setAccessible(true);
			settingsField.set(mainWindow, buildSettings());

			Field eventsField = MainWindow.class.getDeclaredField("events");
			eventsField.setAccessible(true);
			eventsField.set(mainWindow, new JadxGuiEventsImpl());

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

	static JadxSettings buildSettings() {
		JadxSettings settings = new JadxSettings(JadxSettings.buildConfigAdapter());
		settings.loadSettingsFromJsonString("{}");
		return settings;
	}
}
