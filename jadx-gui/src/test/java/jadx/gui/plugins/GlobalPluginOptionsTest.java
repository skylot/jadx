package jadx.gui.plugins;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.OptionFlag;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobalPluginOptionsTest {

	@Test
	public void perProjectOptionNotAllowed() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		TestPlugin plugin = new TestPlugin("global-plugin", OptionFlag.PER_PROJECT);
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(plugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(plugin.globalInitCount).isZero();
		assertThat(manager.getGlobalPlugins()).isEmpty();
	}

	@Test
	public void globalOptionAllowed() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		TestPlugin plugin = new TestPlugin("global-plugin", OptionFlag.NOT_CHANGING_CODE);
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(plugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(plugin.globalInitCount).isEqualTo(1);
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("global-plugin");
		assertThat(manager.getGlobalPlugins().first().getOptions()).isNotNull();
	}

	private static final class TestPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;
		private final OptionFlag optionFlag;
		int globalInitCount;

		private TestPlugin(String pluginId, OptionFlag optionFlag) {
			this.pluginId = pluginId;
			this.optionFlag = optionFlag;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public JadxPluginOptions buildOptions() {
			return new BasePluginOptionsBuilder() {
				@Override
				public void registerOptions() {
					strOption(pluginId + ".test")
							.description("test option")
							.defaultValue("")
							.flags(optionFlag)
							.setter(v -> {
							});
				}
			};
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
			globalInitCount++;
		}
	}
}
