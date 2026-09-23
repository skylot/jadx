package jadx.gui.plugins;

import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.core.plugins.PluginContext;
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

		// shim has no settings so load will fail inside and log an error
		manager.load();

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			assertThatCode(() -> manager.injectGlobalPlugins(projectDecompiler)).doesNotThrowAnyException();
		}
		assertThatCode(manager::getGlobalPluginContexts).doesNotThrowAnyException();
		assertThat(manager.getGlobalPluginContexts()).isEmpty();
		assertThatCode(manager::runGlobalUnload).doesNotThrowAnyException();
	}

	@Test
	public void globalPluginsLoadedWithoutDecompiler() {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin globalPlugin = new RecordingGlobalPlugin("global-plugin");
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(globalPlugin));

		assertThat(manager.getGlobalPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("global-plugin");
		PluginContext globalContext = manager.getGlobalPluginContexts().first();
		// global plugin data not bound to any decompiler, plugin gets only gui context on global init
		assertThat(globalContext).isNotInstanceOf(JadxPluginContext.class);
		assertThat(globalPlugin.globalInitCount).isEqualTo(1);
		assertThat(globalPlugin.globalContext).isSameAs(globalContext.getGuiContext());
		assertThat(globalPlugin.projectInitCount).isZero();
	}

	@Test
	public void disabledGlobalPluginsSkipped() {
		JadxArgs args = new JadxArgs();
		args.getDisabledPlugins().add("disabled-plugin");
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin disabledPlugin = new RecordingGlobalPlugin("disabled-plugin");
		manager.loadGlobalPlugins(args, new TestPluginsLoader(disabledPlugin, new RecordingGlobalPlugin("enabled-plugin")));

		assertThat(manager.getGlobalPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("enabled-plugin");
		assertThat(disabledPlugin.globalInitCount).isZero();
	}

	@Test
	public void globalPluginOptionsAppliedFromArgs() {
		RecordingGlobalPlugin globalPlugin = new RecordingGlobalPlugin("global-plugin");
		JadxArgs args = new JadxArgs();
		args.getPluginOptions().put(globalPlugin.getOptionName(), "global-value");
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		manager.loadGlobalPlugins(args, new TestPluginsLoader(globalPlugin));

		assertThat(globalPlugin.optionValue).isEqualTo("global-value");
		assertThat(manager.getGlobalPluginContexts().first().getOptions()).isNotNull();
	}

	@Test
	public void loadedGlobalPluginsInjectedIntoProject() {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin globalPlugin = new RecordingGlobalPlugin("global-plugin");
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(globalPlugin));
		PluginContext globalContext = manager.getGlobalPluginContexts().first();

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(projectDecompiler);
			manager.injectGlobalPlugins(projectDecompiler);

			assertThat(projectDecompiler.getPluginManager().getAllPluginContexts())
					.extracting(PluginContext::getPluginId)
					.containsExactly("global-plugin");

			// same plugin instance is used in both scopes, but with separate plugin data
			PluginContext projectContext = projectDecompiler.getPluginManager().getAllPluginContexts().first();
			assertThat(projectContext.getPluginInstance()).isSameAs(globalPlugin);
			assertThat(projectContext).isNotSameAs(globalContext);
		}
	}

	@Test
	public void projectOptionsAppliedToInjectedGlobalPlugin() {
		RecordingGlobalPlugin globalPlugin = new RecordingGlobalPlugin("global-plugin");
		JadxArgs globalArgs = new JadxArgs();
		globalArgs.getPluginOptions().put(globalPlugin.getOptionName(), "global-value");
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		manager.loadGlobalPlugins(globalArgs, new TestPluginsLoader(globalPlugin));

		// project args contain per project options (see JadxProject.fillJadxArgs)
		JadxArgs projectArgs = new JadxArgs();
		projectArgs.getPluginOptions().put(globalPlugin.getOptionName(), "project-value");
		try (JadxDecompiler projectDecompiler = new JadxDecompiler(projectArgs)) {
			manager.initGuiPluginsContext(projectDecompiler);
			manager.injectGlobalPlugins(projectDecompiler);

			PluginContext projectContext = projectDecompiler.getPluginManager().getAllPluginContexts().first();
			assertThat(projectContext.getOptions()).isNotNull();
			assertThat(globalPlugin.optionValue).isEqualTo("project-value");
		}
	}

	@Test
	public void globalPluginCustomSettingsUsedInProjectScope() {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		RecordingGlobalPlugin globalPlugin = new RecordingGlobalPlugin("global-plugin");
		ISettingsGroup settingsGroup = new TestSettingsGroup();
		globalPlugin.customSettings = settingsGroup;
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(globalPlugin));

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(projectDecompiler);
			manager.injectGlobalPlugins(projectDecompiler);

			PluginContext projectContext = projectDecompiler.getPluginManager().getAllPluginContexts().first();
			GuiPluginContext projectGuiContext = (GuiPluginContext) projectContext.getGuiContext();
			assertThat(projectGuiContext).isNotNull();
			assertThat(projectGuiContext.getCustomSettingsGroup()).isSameAs(settingsGroup);
		}
	}

	@Test
	public void projectPluginCustomSettingsNotAffected() {
		GuiPluginsManager manager = new GuiPluginsManager(TestMainWindowShim.build());
		manager.loadGlobalPlugins(new JadxArgs(), new TestPluginsLoader(new RecordingGlobalPlugin("global-plugin")));

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(projectDecompiler);
			PluginContext projectOnly = projectDecompiler.getPluginManager().register(new TestPlugin("project-plugin"));
			assertThat(projectOnly).isNotNull();
			GuiPluginContext projectOnlyGui = (GuiPluginContext) projectOnly.getGuiContext();
			assertThat(projectOnlyGui).isNotNull();
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

	private static final class TestPlugin implements JadxPlugin {
		private final String pluginId;

		private TestPlugin(String pluginId) {
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
}
