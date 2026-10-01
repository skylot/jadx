package jadx.gui.plugins;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalGuiPluginLifecycleTest {

	@Test
	public void lifecycleAcrossSeveralProjects() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingPlugin plugin = new CountingPlugin();

		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(plugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(plugin.globalInitCount).isEqualTo(1);
		assertThat(plugin.projectInitCount).isEqualTo(0);
		assertThat(plugin.unloadCount).isEqualTo(0);
		assertThat(plugin.globalUnloadCount).isEqualTo(0);

		// open and close two projects
		for (int i = 1; i <= 2; i++) {
			try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
				manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
				manager.injectGlobalPlugins(projectDecompiler);
				projectDecompiler.getPluginManager().initResolved(projectDecompiler);

				assertThat(plugin.projectInitCount).isEqualTo(i);
				assertThat(projectDecompiler.getPluginManager().getAllPlugins().first()
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
