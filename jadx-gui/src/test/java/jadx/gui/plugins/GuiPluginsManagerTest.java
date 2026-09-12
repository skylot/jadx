package jadx.gui.plugins;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.junit.jupiter.api.Test;

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
	public void loadedGlobalPluginsInjectedIntoProject() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		try (JadxDecompiler globalDecompiler = new JadxDecompiler();
				JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			assertThat(globalDecompiler.getPluginManager().register(globalPlugin)).isNotNull();
			setGlobalDecompiler(manager, globalDecompiler);

			assertThat(manager.getGlobalPluginContexts())
					.extracting(PluginContext::getPluginId)
					.containsExactly("global-plugin");

			manager.injectGlobalPlugins(projectDecompiler);

			assertThat(projectDecompiler.getPluginManager().getAllPluginContexts())
					.extracting(PluginContext::getPluginId)
					.containsExactly("global-plugin");

			// same plugin instance is used in both scopes
			PluginContext projectContext = projectDecompiler.getPluginManager().getAllPluginContexts().first();
			assertThat(projectContext.getPluginInstance()).isSameAs(globalPlugin);
		}
	}

	@Test
	public void globalPluginCustomSettingsUsedInProjectScope() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		try (JadxDecompiler globalDecompiler = new JadxDecompiler();
				JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			manager.initGuiPluginsContext(globalDecompiler, true);
			PluginContext globalContext = globalDecompiler.getPluginManager().register(globalPlugin);
			assertThat(globalContext).isNotNull();
			GuiPluginContext globalGuiContext = (GuiPluginContext) globalContext.getGuiContext();
			ISettingsGroup settingsGroup = new TestSettingsGroup();
			globalGuiContext.settings().setCustomSettingsGroup(settingsGroup);
			setGlobalDecompiler(manager, globalDecompiler);

			manager.initGuiPluginsContext(projectDecompiler, false);
			manager.injectGlobalPlugins(projectDecompiler);

			PluginContext projectContext = projectDecompiler.getPluginManager().getAllPluginContexts().first();
			GuiPluginContext projectGuiContext = (GuiPluginContext) projectContext.getGuiContext();
			assertThat(projectGuiContext.getCustomSettingsGroup()).isSameAs(settingsGroup);
		}
	}

	@Test
	public void projectPluginCustomSettingsNotAffected() throws Exception {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		try (JadxDecompiler globalDecompiler = new JadxDecompiler();
				JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(globalDecompiler, true);
			assertThat(globalDecompiler.getPluginManager().register(new TestPlugin("global-plugin"))).isNotNull();
			setGlobalDecompiler(manager, globalDecompiler);

			manager.initGuiPluginsContext(projectDecompiler, false);
			PluginContext projectOnly = projectDecompiler.getPluginManager().register(new TestPlugin("project-plugin"));
			assertThat(projectOnly).isNotNull();
			GuiPluginContext projectOnlyGui = (GuiPluginContext) projectOnly.getGuiContext();
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

	private static void setGlobalDecompiler(GuiPluginsManager manager, JadxDecompiler decompiler) throws Exception {
		Field field = GuiPluginsManager.class.getDeclaredField("globalDecompiler");
		field.setAccessible(true);
		field.set(manager, decompiler);
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
