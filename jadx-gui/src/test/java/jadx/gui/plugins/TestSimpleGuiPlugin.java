package jadx.gui.plugins;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.gui.plugins.JadxGuiPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class TestSimpleGuiPlugin extends JadxGuiPlugin {
	private static final Logger LOG = LoggerFactory.getLogger(TestSimpleGuiPlugin.class);
	private static final String PLUGIN_ID = "test-gui-plugin";

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("Test simple GUI plugin")
				.description("test simple plugin")
				.build();
	}

	@Override
	public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
		LOG.info("TestSimpleGuiPlugin init called");
	}

	@Override
	public JadxPluginOptions buildOptions() {
		LOG.info("TestSimpleGuiPlugin buildOptions called");
		return new BasePluginOptionsBuilder() {
			@Override
			public void registerOptions() {
				strOption(PLUGIN_ID + ".test")
						.description("sample option")
						.defaultValue("test")
						.setter(v -> LOG.info("TestSimpleGuiPlugin set test option to: {}", v));
			}
		};
	}

	@Override
	public void unload() {
		LOG.info("TestSimpleGuiPlugin unload called");
	}
}
