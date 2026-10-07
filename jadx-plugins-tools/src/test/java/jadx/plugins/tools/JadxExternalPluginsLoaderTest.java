package jadx.plugins.tools;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxExternalPluginsLoaderTest {

	@TempDir
	Path tempDir;

	@Test
	public void releaseJarWithoutAcceptedPlugins() throws Exception {
		Path pluginJar = tempDir.resolve("test-plugin.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(pluginJar))) {
			out.putNextEntry(new JarEntry("META-INF/services/" + JadxPlugin.class.getName()));
			out.write(TestPlugin.class.getName().getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(cls -> false)) {
			assertThatThrownBy(() -> loader.loadFromPath(pluginJar))
					.hasMessageContaining("No plugin found in jar");
			Files.delete(pluginJar);
		}
	}

	public static class TestPlugin implements JadxPlugin {
		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("test-plugin").name("Test plugin").description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}
}
