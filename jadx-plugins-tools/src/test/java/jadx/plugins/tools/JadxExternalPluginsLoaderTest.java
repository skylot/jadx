package jadx.plugins.tools;

import java.io.IOException;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxExternalPluginsLoaderTest {
	private static final String SERVICE_FILE = "META-INF/services/" + JadxPlugin.class.getName();

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

	@Test
	public void closeLoadedPluginClassLoader() throws Exception {
		Path pluginJar = buildPluginJar();
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader()) {
			List<JadxPlugin> plugins = loader.loadPluginsFromPath(pluginJar);
			assertThat(plugins).hasSize(1);
			JadxPlugin plugin = plugins.get(0);
			assertThat(plugin.getPluginInfo().getPluginId()).isEqualTo("jar-test-plugin");
			URLClassLoader pluginClsLoader = (URLClassLoader) plugin.getClass().getClassLoader();
			assertThat(pluginClsLoader.findResource(SERVICE_FILE)).isNotNull();

			loader.closeClassLoader(plugin);
			assertThat(pluginClsLoader.findResource(SERVICE_FILE)).isNull();
			Files.delete(pluginJar);
		}
	}

	@Test
	public void filteredPluginsNotLoadedFromPath() throws Exception {
		Path pluginJar = buildPluginJar();
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(cls -> false)) {
			assertThat(loader.loadPluginsFromPath(pluginJar)).isEmpty();
			Files.delete(pluginJar);
		}
	}

	private Path buildPluginJar() throws IOException {
		Path srcFile = tempDir.resolve("src/jartest/JarTestPlugin.java");
		Files.createDirectories(srcFile.getParent());
		Files.writeString(srcFile, String.join("\n",
				"package jartest;",
				"import jadx.api.plugins.*;",
				"public class JarTestPlugin implements JadxPlugin {",
				"	public JadxPluginInfo getPluginInfo() {",
				"		return JadxPluginInfoBuilder.pluginId(\"jar-test-plugin\").name(\"jar test\").description(\"test\").build();",
				"	}",
				"	public void init(JadxPluginContext context) {",
				"	}",
				"}"));
		Path classesDir = tempDir.resolve("classes");
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int result = compiler.run(null, null, null,
				"-cp", System.getProperty("java.class.path"),
				"-d", classesDir.toString(), srcFile.toString());
		assertThat(result).isZero();

		Path pluginJar = tempDir.resolve("jar-test-plugin.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(pluginJar))) {
			out.putNextEntry(new JarEntry("jartest/JarTestPlugin.class"));
			out.write(Files.readAllBytes(classesDir.resolve("jartest/JarTestPlugin.class")));
			out.closeEntry();
			out.putNextEntry(new JarEntry(SERVICE_FILE));
			out.write("jartest.JarTestPlugin".getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		return pluginJar;
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
