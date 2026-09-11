package jadx.gui.plugins;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;

public class TestGlobalGuiPlugin extends JadxGlobalGuiPlugin {
	private static final Logger LOG = LoggerFactory.getLogger(TestGlobalGuiPlugin.class);
	private static final String PLUGIN_ID = "test-gui-global-plugin";

	public TestGlobalGuiPlugin() {
		LOG.info("TestGlobalGuiPlugin constructor called");
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId(PLUGIN_ID)
				.name("Test global GUI plugin")
				.description("test global plugin")
				.build();
	}

	@Override
	public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContextExt) {
		LOG.info("TestGlobalGuiPlugin globalInit called");
	}

	@Override
	public JadxPluginOptions buildOptions() {
		LOG.info("TestGlobalGuiPlugin buildOptions called");
		return new BasePluginOptionsBuilder() {
			@Override
			public void registerOptions() {
				strOption(PLUGIN_ID + ".test")
						.description("global sample option")
						.defaultValue("sample")
						.setter(v -> LOG.info("TestGlobalGuiPlugin set test option to: {}", v));
			}
		};
	}

	@Override
	public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
		LOG.info("TestGlobalGuiPlugin project init called");
	}

	@Override
	public void unload() {
		LOG.info("TestGlobalGuiPlugin project unload call");
	}

	@Override
	public void globalUnload() {
		LOG.info("TestGlobalGuiPlugin globalUnload called");
	}
}
