package jadx.gui.utils.plugins;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.cli.plugins.JadxFilesGetter;
import jadx.core.utils.files.FileUtils;
import jadx.gui.plugins.GuiPluginsManager;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;

import static org.assertj.core.api.Assertions.assertThat;

public class CollectPluginsTest {

	@Test
	public void keepSharedTempFiles() {
		MainWindow mainWindow = TestMainWindowShim.build();
		CountingPlugin plugin = new CountingPlugin();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow) {
			@Override
			public JadxPluginLoader buildProjectPluginLoader() {
				return new TestPluginLoader(plugin);
			}
		};
		TestMainWindowShim.setPluginsManager(mainWindow, manager);

		// same temp dirs as in jadx-gui after project load
		FileUtils.updateTempRootDir(JadxFilesGetter.INSTANCE.getTempDir());
		Path tempFile = FileUtils.createTempFile(".tmp");

		new CollectPlugins(mainWindow).build();
		assertThat(tempFile).exists();
		assertThat(plugin.initCount).isEqualTo(1);
		assertThat(plugin.unloadCount).isEqualTo(1);
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

	private static final class CountingPlugin implements JadxPlugin {
		int initCount;
		int unloadCount;

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("counting-plugin").name("counting").description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
			initCount++;
		}

		@Override
		public void unload() {
			unloadCount++;
		}
	}
}
