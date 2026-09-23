package jadx.gui.plugins;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.gui.IMainWindow;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class MainWindowApiTest {

	@Test
	public void mainWindowImplementsApi() {
		assertThat(IMainWindow.class).isAssignableFrom(MainWindow.class);
	}

	@Test
	public void apiNotExposeGuiInternals() {
		List<Class<?>> usedTypes = new ArrayList<>();
		for (Method method : IMainWindow.class.getMethods()) {
			collectTypes(method.getGenericReturnType(), usedTypes);
			for (Type paramType : method.getGenericParameterTypes()) {
				collectTypes(paramType, usedTypes);
			}
		}
		assertThat(usedTypes).isNotEmpty();
		assertThat(usedTypes).allSatisfy(cls -> assertThat(cls.getName()).doesNotStartWith("jadx.gui."));
	}

	@Test
	public void globalPluginGetsMainWindow() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		RecordingGlobalPlugin plugin = new RecordingGlobalPlugin("global-plugin");
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(plugin));

		assertThat(plugin.globalContext).isNotNull();
		IMainWindow pluginMainWindow = plugin.globalContext.getMainWindow();
		assertThat(pluginMainWindow).isSameAs(mainWindow);
	}

	private static void collectTypes(Type type, List<Class<?>> types) {
		if (type instanceof Class) {
			types.add((Class<?>) type);
		} else if (type instanceof ParameterizedType) {
			ParameterizedType parameterizedType = (ParameterizedType) type;
			collectTypes(parameterizedType.getRawType(), types);
			for (Type argType : parameterizedType.getActualTypeArguments()) {
				collectTypes(argType, types);
			}
		}
	}
}
