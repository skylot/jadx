package jadx.gui.plugins;

import java.util.concurrent.atomic.AtomicInteger;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

public class TestSimpleProbePlugin implements JadxPlugin {

	public static final AtomicInteger CONSTRUCT_COUNT = new AtomicInteger();

	public TestSimpleProbePlugin() {
		CONSTRUCT_COUNT.incrementAndGet();
	}

	@Override
	public JadxPluginInfo getPluginInfo() {
		return JadxPluginInfoBuilder.pluginId("test-simple-probe")
				.name("Test simple probe")
				.description("test")
				.build();
	}

	@Override
	public void init(JadxPluginContext context) {
		// no-op
	}
}
