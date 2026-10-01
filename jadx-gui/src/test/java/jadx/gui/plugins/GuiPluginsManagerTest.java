package jadx.gui.plugins;

import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GuiPluginsManagerTest {

	@Test
	public void globalPluginsLoadFailureDontBreakProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		TestPlugin globalPlugin = new TestPlugin("bad-global-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new RuntimeException("test");
			}
		};
		assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();
		manager.load();

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			assertThatCode(() -> manager.injectGlobalPlugins(projectDecompiler)).doesNotThrowAnyException();
		}
		assertThatCode(manager::getGlobalPlugins).doesNotThrowAnyException();
		assertThat(manager.getGlobalPlugins().stream().filter(PluginRuntime::isInitialized)).isEmpty();
		assertThatCode(manager::runGlobalUnload).doesNotThrowAnyException();
	}

	@Test
	public void loadedGlobalPluginsInjectedIntoProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();

			assertThat(manager.getGlobalPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("global-plugin");

			manager.injectGlobalPlugins(projectDecompiler);

			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("global-plugin");

			// same plugin instance is used in both scopes
			PluginRuntime projectPlugin = projectDecompiler.getPluginManager().getAllPlugins().first();
			assertThat(projectPlugin.getPluginInstance()).isSameAs(globalPlugin);
		}
	}

	@Test
	public void globalPluginCustomSettingsUsedInProjectScope() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			manager.initGuiPluginsContextForGlobalScope();
			PluginRuntime globalPluginRuntime = manager.getGlobalPluginManager().register(globalPlugin);
			assertThat(globalPluginRuntime).isNotNull();
			GuiPluginContext globalGuiContext = (GuiPluginContext) globalPluginRuntime.getAppContext().getGuiContext();
			ISettingsGroup settingsGroup = new TestSettingsGroup();
			globalGuiContext.settings().setCustomSettingsGroup(settingsGroup);

			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			manager.injectGlobalPlugins(projectDecompiler);

			PluginRuntime projectPlugin = projectDecompiler.getPluginManager().getAllPlugins().first();
			GuiPluginContext projectGuiContext = (GuiPluginContext) projectPlugin.getAppContext().getGuiContext();
			assertThat(projectGuiContext.getCustomSettingsGroup()).isSameAs(settingsGroup);
		}
	}

	@Test
	public void projectPluginCustomSettingsNotAffected() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		JadxArgs jadxArgs = new JadxArgs();
		JadxPluginManager globalPluginManager = new JadxPluginManager(jadxArgs);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(globalPluginManager, jadxArgs, true);
			assertThat(globalPluginManager.register(new TestPlugin("global-plugin"))).isNotNull();

			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			PluginRuntime projectOnly = projectDecompiler.getPluginManager().register(new TestPlugin("project-plugin"));
			assertThat(projectOnly).isNotNull();
			GuiPluginContext projectOnlyGui = (GuiPluginContext) projectOnly.getAppContext().getGuiContext();
			ISettingsGroup ownGroup = new TestSettingsGroup();
			projectOnlyGui.settings().setCustomSettingsGroup(ownGroup);

			manager.injectGlobalPlugins(projectDecompiler);

			assertThat(projectOnlyGui.getCustomSettingsGroup()).isSameAs(ownGroup);
		}
	}

	private static final class TestSettingsGroup implements ISettingsGroup {
		@Override
		public String getTitle() {
			return "custom";
		}

		@Override
		public JComponent buildComponent() {
			return new JLabel();
		}

		@Override
		public List<ISettingsGroup> getSubGroups() {
			return Collections.emptyList();
		}
	}

	private static class TestPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;

		private TestPlugin(String pluginId) {
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
		public void init(JadxPluginContext context) {
		}
	}
}
