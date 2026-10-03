package jadx.gui.plugins;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.plugins.CollectPlugins;

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
	public void failedGlobalPluginNotInjectedIntoProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingGlobalPlugin failedPlugin = new CountingGlobalPlugin("failed-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new RuntimeException("test");
			}
		};
		CountingGlobalPlugin goodPlugin = new CountingGlobalPlugin("good-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(failedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(goodPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("good-plugin");
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			manager.injectGlobalPlugins(projectDecompiler);
			projectDecompiler.getPluginManager().initResolved(projectDecompiler);
			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("good-plugin");
		}
		manager.runGlobalUnload();

		assertThat(failedPlugin.projectInitCount).isZero();
		assertThat(failedPlugin.globalUnloadCount).isZero();
		assertThat(goodPlugin.projectInitCount).isEqualTo(1);
		assertThat(goodPlugin.globalUnloadCount).isEqualTo(1);
	}

	@Test
	public void globalInitErrorDontStopOtherPlugins() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingGlobalPlugin failedPlugin = new CountingGlobalPlugin("a-failed-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new NoClassDefFoundError("test");
			}
		};
		CountingGlobalPlugin goodPlugin = new CountingGlobalPlugin("b-good-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(failedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(goodPlugin)).isNotNull();

		assertThatCode(() -> manager.runGlobalInit(manager.getGlobalPlugins())).doesNotThrowAnyException();
		assertThat(goodPlugin.globalInitCount).isEqualTo(1);
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("b-good-plugin");
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

	@Test
	public void globalPluginsCollectedForSettingsWithoutProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		JadxPlugin projectPlugin = new TestProjectPlugin("project-plugin");
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow) {
			@Override
			public JadxPluginLoader buildProjectPluginLoader() {
				return new TestPluginLoader(projectPlugin);
			}
		};
		TestMainWindowShim.setPluginsManager(mainWindow, manager);

		TestPlugin globalPlugin = new TestPlugin("global-plugin") {
			@Override
			public JadxPluginOptions buildOptions() {
				return new TestOptions("global-plugin");
			}
		};
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		List<PluginRuntime> plugins = new CollectPlugins(mainWindow).build();
		assertThat(plugins)
				.extracting(PluginRuntime::getPluginId)
				.containsExactlyInAnyOrder("global-plugin", "project-plugin");
		assertThat(plugins)
				.filteredOn(p -> p.getPluginId().equals("global-plugin"))
				.allSatisfy(p -> assertThat(p.getOptions()).isNotNull());
	}

	@Test
	public void settingsWindowReloadedAfterGlobalInit() throws InterruptedException {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountDownLatch reloaded = new CountDownLatch(1);
		mainWindow.events().global().addListener(JadxEvents.RELOAD_SETTINGS_WINDOW, ev -> reloaded.countDown());

		assertThat(manager.getGlobalPluginManager().register(new TestPlugin("global-plugin"))).isNotNull();
		manager.load();

		assertThat(reloaded.await(10, TimeUnit.SECONDS)).isTrue();
	}

	private static final class TestPluginLoader implements JadxPluginLoader {
		private final JadxPlugin plugin;

		private TestPluginLoader(JadxPlugin plugin) {
			this.plugin = plugin;
		}

		@Override
		public List<JadxPlugin> load() {
			return Collections.singletonList(plugin);
		}

		@Override
		public void close() {
		}
	}

	private static final class TestProjectPlugin implements JadxPlugin {
		private final String pluginId;

		private TestProjectPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}

	private static final class TestOptions extends BasePluginOptionsBuilder {
		private final String pluginId;

		private TestOptions(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public void registerOptions() {
			strOption(pluginId + ".test")
					.description("test option")
					.defaultValue("")
					.setter(v -> {
					});
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

	private static class CountingGlobalPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;
		int globalInitCount;
		int projectInitCount;
		int globalUnloadCount;

		private CountingGlobalPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
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
		public void globalUnload() {
			globalUnloadCount++;
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
