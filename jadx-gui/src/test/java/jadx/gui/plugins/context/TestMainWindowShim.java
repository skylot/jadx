package jadx.gui.plugins.context;

import java.lang.reflect.Field;

import javax.swing.JMenu;

import org.junit.jupiter.api.Assumptions;

import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;

public class TestMainWindowShim {

	public static MainWindow build() {
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
			return (MainWindow) mainWindow;
		} catch (Throwable e) {
			Assumptions.abort("Can't build MainWindow instance for test: " + e);
			return null;
		}
	}
}
