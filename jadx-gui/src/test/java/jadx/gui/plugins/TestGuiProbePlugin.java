package jadx.gui.plugins;

import java.util.concurrent.atomic.AtomicInteger;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.gui.JadxGuiPlugin;

public class TestGuiProbePlugin implements JadxGuiPlugin {

	public static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

	public TestGuiProbePlugin() {
		CONSTRUCT_COUNT.incrementAndGet();
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId("test-gui-probe")
				.name("Test gui probe")
				.description("test")
				.build();
	}

	@Override
	public void initGui(JadxGuiContext context) {
		// no-op
	}
}
