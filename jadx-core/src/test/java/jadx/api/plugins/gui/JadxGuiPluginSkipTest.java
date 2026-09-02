package jadx.api.plugins.gui;

import org.junit.jupiter.api.Test;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class JadxGuiPluginSkipTest {

	@Test
	public void guiPluginNotAddedToPluginManager() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			decompiler.registerPlugin(new TestGuiPlugin());
			assertThat(decompiler.getPluginManager().getAllPluginContexts()).isEmpty();
			assertThat(decompiler.getPluginManager().getResolvedPluginContexts()).isEmpty();
		}
	}

	@Test
	public void simplePluginAddedToPluginManager() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			decompiler.registerPlugin(new TestSimplePlugin());
			assertThat(decompiler.getPluginManager().getAllPluginContexts())
					.extracting(p -> p.getPluginId())
					.containsExactly("test-simple-plugin");
		}
	}

	private static class TestGuiPlugin implements JadxGuiPlugin {
		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("test-gui-plugin")
					.name("Test gui plugin")
					.description("test")
					.build();
		}

		@Override
		public void initGui(JadxGuiContext context) {
			throw new AssertionError("Should not be called in this test");
		}
	}

	private static class TestSimplePlugin implements JadxPlugin {
		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("test-simple-plugin")
					.name("Test simple plugin")
					.description("test")
					.build();
		}

		@Override
		public void init(JadxPluginContext context) {
			// no-op
		}
	}
}
