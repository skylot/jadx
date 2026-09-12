package jadx.gui.plugins;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.SortedSet;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.core.plugins.PluginContext;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalGuiPluginLifecycleTest {

	@Test
	public void lifecycleAcrossSeveralProjects() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingPlugin plugin = new CountingPlugin();

		try (JadxDecompiler globalDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(globalDecompiler, true);
			assertThat(globalDecompiler.getPluginManager().register(plugin)).isNotNull();
			setGlobalDecompiler(manager, globalDecompiler);
			runGlobalInit(manager, manager.getGlobalPluginContexts());

			assertThat(plugin.globalInitCount).isEqualTo(1);
			assertThat(plugin.projectInitCount).isEqualTo(0);
			assertThat(plugin.unloadCount).isEqualTo(0);
			assertThat(plugin.globalUnloadCount).isEqualTo(0);

			// open and close two projects
			for (int i = 1; i <= 2; i++) {
				try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
					manager.initGuiPluginsContext(projectDecompiler, false);
					manager.injectGlobalPlugins(projectDecompiler);
					projectDecompiler.getPluginManager().initResolved();

					assertThat(plugin.projectInitCount).isEqualTo(i);
					assertThat(projectDecompiler.getPluginManager().getAllPluginContexts().first()
							.getPluginInstance()).isSameAs(plugin);
				}
				assertThat(plugin.unloadCount).isEqualTo(i);
				assertThat(plugin.globalInitCount).isEqualTo(1);
				assertThat(plugin.globalUnloadCount).isEqualTo(0);
			}

			// jadx-gui exit
			manager.runGlobalUnload();
			assertThat(plugin.globalUnloadCount).isEqualTo(1);
			assertThat(plugin.globalInitCount).isEqualTo(1);
			assertThat(plugin.projectInitCount).isEqualTo(2);
			assertThat(plugin.unloadCount).isEqualTo(2);
		}
	}

	private static void runGlobalInit(GuiPluginsManager manager, SortedSet<PluginContext> plugins) throws Exception {
		Method method = GuiPluginsManager.class.getDeclaredMethod("runGlobalInit", SortedSet.class);
		method.setAccessible(true);
		method.invoke(manager, plugins);
	}

	private static void setGlobalDecompiler(GuiPluginsManager manager, JadxDecompiler decompiler) throws Exception {
		Field field = GuiPluginsManager.class.getDeclaredField("globalDecompiler");
		field.setAccessible(true);
		field.set(manager, decompiler);
	}

	private static final class CountingPlugin extends JadxGlobalGuiPlugin {
		int globalInitCount;
		int projectInitCount;
		int unloadCount;
		int globalUnloadCount;

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("counting-global-plugin")
					.name("counting").description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
			globalInitCount++;
		}

		@Override
		public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
			projectInitCount++;
		}

		@Override
		public void unload() {
			unloadCount++;
		}

		@Override
		public void globalUnload() {
			globalUnloadCount++;
		}
	}
}
