package jadx.gui.plugins;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.gui.JadxGuiContext;
import jadx.api.plugins.gui.JadxGuiPlugin;
import jadx.core.plugins.versions.VerifyRequiredVersion;
import jadx.plugins.tools.JadxExternalPluginsLoader;

import static org.assertj.core.api.Assertions.assertThat;

public class GuiPluginsManagerTest {

	private static final VerifyRequiredVersion VERSION_1_5_1 = new VerifyRequiredVersion("1.5.1");

	@Test
	public void loaderFilterSkipsNotGuiPluginsBeforeConstruction() {
		int simpleConstructsBefore = TestSimpleProbePlugin.CONSTRUCT_COUNT.get();
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader()) {
			List<JadxPlugin> plugins = loader.load(JadxGuiPlugin.class::isAssignableFrom);

			assertThat(plugins).isNotEmpty();
			assertThat(plugins).allMatch(p -> p instanceof JadxGuiPlugin);
			assertThat(plugins).extracting(p -> p.getPluginInfo().getPluginId()).contains("test-gui-probe");
			assertThat(TestSimpleProbePlugin.CONSTRUCT_COUNT.get()).isEqualTo(simpleConstructsBefore);
		}
	}

	@Test
	public void loaderWithoutFilterLoadsSimplePlugins() {
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader()) {
			List<JadxPlugin> plugins = loader.load();
			assertThat(plugins).extracting(p -> p.getPluginInfo().getPluginId())
					.contains("test-simple-probe", "test-gui-probe");
		}
	}

	@Test
	public void onlyGuiPluginsInitialized() {
		TestPlugin guiPlugin = new TestPlugin("gui", null);
		JadxPlugin simplePlugin = new TestSimpleProbePlugin();

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(guiPlugin, simplePlugin), Collections.emptySet(), VERSION_1_5_1, info -> null);

		assertThat(initialized).containsExactly(guiPlugin);
		assertThat(guiPlugin.initCount).isEqualTo(1);
	}

	@Test
	public void disabledPluginNotInitialized() {
		TestPlugin disabled = new TestPlugin("disabled", null);
		TestPlugin enabled = new TestPlugin("enabled", null);

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(disabled, enabled), Collections.singleton("disabled"), VERSION_1_5_1, info -> null);

		assertThat(initialized).containsExactly(enabled);
		assertThat(disabled.initCount).isEqualTo(0);
	}

	@Test
	public void incompatiblePluginNotInitialized() {
		TestPlugin incompatible = new TestPlugin("incompatible", "1.5.2, r2400");
		TestPlugin compatible = new TestPlugin("compatible", "1.5.0, r2200");

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(incompatible, compatible), Collections.emptySet(), VERSION_1_5_1, info -> null);

		assertThat(initialized).containsExactly(compatible);
		assertThat(incompatible.initCount).isEqualTo(0);
	}

	@Test
	public void initFailureDontStopOtherPlugins() {
		TestPlugin failing = new TestPlugin("failing", null);
		failing.failOnInit = true;
		TestPlugin second = new TestPlugin("second", null);

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(failing, second), Collections.emptySet(), VERSION_1_5_1, info -> null);

		assertThat(initialized).containsExactly(second);
		assertThat(second.initCount).isEqualTo(1);
	}

	@Test
	public void initializedPluginsUnloadedOnce() {
		TestPlugin failing = new TestPlugin("failing", null);
		failing.failOnInit = true;
		TestPlugin first = new TestPlugin("first", null);
		TestPlugin second = new TestPlugin("second", null);

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(first, failing, second), Collections.emptySet(), VERSION_1_5_1, info -> null);
		GuiPluginsManager.unloadPlugins(initialized);

		assertThat(first.unloadCount).isEqualTo(1);
		assertThat(second.unloadCount).isEqualTo(1);
		// not initialized plugin should not be unloaded
		assertThat(failing.unloadCount).isEqualTo(0);
	}

	@Test
	public void unloadFailureDontStopOtherPlugins() {
		TestPlugin failing = new TestPlugin("failing", null);
		failing.failOnUnload = true;
		TestPlugin second = new TestPlugin("second", null);

		List<JadxGuiPlugin> initialized = GuiPluginsManager.initPlugins(
				Arrays.asList(failing, second), Collections.emptySet(), VERSION_1_5_1, info -> null);
		GuiPluginsManager.unloadPlugins(new ArrayList<>(initialized));

		assertThat(second.unloadCount).isEqualTo(1);
	}

	private static class TestPlugin implements JadxGuiPlugin {
		private final String id;
		private final String requiredVersion;

		boolean failOnInit;
		boolean failOnUnload;
		int initCount;
		int unloadCount;

		TestPlugin(String id, String requiredVersion) {
			this.id = id;
			this.requiredVersion = requiredVersion;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			JadxPluginInfoBuilder builder = JadxPluginInfoBuilder.pluginId(id).name(id).description("test");
			if (requiredVersion != null) {
				builder.requiredJadxVersion(requiredVersion);
			}
			return builder.build();
		}

		@Override
		public void initGui(JadxGuiContext context) {
			if (failOnInit) {
				throw new RuntimeException("init failed");
			}
			initCount++;
		}

		@Override
		public void unload() {
			if (failOnUnload) {
				throw new RuntimeException("unload failed");
			}
			unloadCount++;
		}
	}
}
