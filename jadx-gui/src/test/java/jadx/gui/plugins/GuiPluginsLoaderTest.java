package jadx.gui.plugins;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GuiPluginsLoaderTest {

	@Test
	public void projectPluginWithGlobalIdSkipped() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		GlobalPlugin globalPlugin = new GlobalPlugin("same-id");
		manager.initGuiPluginsContextForGlobalScope();
		PluginRuntime globalRuntime = manager.getGlobalPluginManager().register(globalPlugin);
		assertThat(globalRuntime).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		ProjectPlugin projectPlugin = new ProjectPlugin("same-id");
		GuiPluginsLoader loader = new GuiPluginsLoader(manager.getGlobalPlugins(), new TestPluginLoader(projectPlugin));
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = projectDecompiler.getPluginManager();
			manager.initGuiPluginsContext(pluginManager, projectDecompiler.getArgs(), false);
			pluginManager.load(loader);
			pluginManager.initResolved(projectDecompiler);

			assertThat(pluginManager.getAllPlugins())
					.extracting(PluginRuntime::getPluginInstance)
					.containsExactly(globalPlugin);
			assertThat(globalPlugin.projectInitCount).isEqualTo(1);
			assertThat(projectPlugin.initCount).isZero();
			// options shared with global plugin
			assertThat(pluginManager.getAllPlugins().first().getOptions()).isSameAs(globalRuntime.getOptions());

			// same after repeated load (passes reload)
			pluginManager.load(loader);
			assertThat(pluginManager.getAllPlugins())
					.extracting(PluginRuntime::getPluginInstance)
					.containsExactly(globalPlugin);
		}
	}

	private static final class GlobalPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;
		int projectInitCount;

		private GlobalPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
		}

		@Override
		public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
			projectInitCount++;
		}
	}

	private static final class ProjectPlugin implements JadxPlugin {
		private final String pluginId;
		int initCount;

		private ProjectPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
			initCount++;
		}
	}
}
