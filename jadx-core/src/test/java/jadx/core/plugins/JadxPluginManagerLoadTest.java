package jadx.core.plugins;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.loader.JadxPluginLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JadxPluginManagerLoadTest {

	private static final String SMALI = ".class Lsmali/HelloWorld;\n"
			+ ".super Ljava/lang/Object;\n"
			+ ".method constructor <init>()V\n"
			+ "    .registers 1\n"
			+ "    invoke-direct {p0}, Ljava/lang/Object;-><init>()V\n"
			+ "    return-void\n"
			+ ".end method\n";

	@TempDir
	Path tmp;

	@Test
	public void repeatedLoadDontFail() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			TestLoader loader = new TestLoader(new TestPlugin("test-plugin"));

			pluginManager.load(loader);
			assertThat(pluginManager.getAllPluginContexts()).hasSize(1);

			assertThatCode(() -> pluginManager.load(loader)).doesNotThrowAnyException();
			assertThat(pluginManager.getAllPluginContexts()).hasSize(1);
		}
	}

	// Plugins added with 'register' method (i.e global gui plugins) should survive plugins load
	@Test
	public void loadKeepRegisteredPlugins() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			assertThat(pluginManager.register(new TestPlugin("registered-plugin"))).isNotNull();

			pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));

			assertThat(pluginManager.getAllPluginContexts())
					.extracting(PluginContext::getPluginId)
					.containsExactlyInAnyOrder("registered-plugin", "loaded-plugin");

			// and still kept after a repeated load
			pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));
			assertThat(pluginManager.getAllPluginContexts())
					.extracting(PluginContext::getPluginId)
					.containsExactlyInAnyOrder("registered-plugin", "loaded-plugin");
		}
	}

	@Test
	public void duplicatedPluginIdRejected() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			assertThatThrownBy(() -> pluginManager.load(
					new TestLoader(new TestPlugin("same-id"), new OtherTestPlugin("same-id"))))
							.isInstanceOf(IllegalArgumentException.class)
							.hasMessageContaining("Duplicate plugin id");
		}
	}

	@Test
	public void reloadPassesDontFail() throws Exception {
		Path smali = tmp.resolve("HelloWorld.smali");
		Files.write(smali, SMALI.getBytes("UTF-8"));
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(smali.toFile());
		args.setOutDir(tmp.resolve("out").toFile());

		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			decompiler.load();
			int pluginsCount = decompiler.getPluginManager().getAllPluginContexts().size();
			assertThat(pluginsCount).isPositive();

			assertThatCode(decompiler::reloadPasses).doesNotThrowAnyException();
			assertThat(decompiler.getPluginManager().getAllPluginContexts()).hasSize(pluginsCount);

			assertThatCode(decompiler::reloadPasses).doesNotThrowAnyException();
			assertThat(decompiler.getPluginManager().getAllPluginContexts()).hasSize(pluginsCount);
		}
	}

	private static final class TestLoader implements JadxPluginLoader {
		private final List<JadxPlugin> plugins;

		private TestLoader(JadxPlugin... plugins) {
			this.plugins = Arrays.asList(plugins);
		}

		@Override
		public List<JadxPlugin> load() {
			return new ArrayList<>(plugins);
		}

		@Override
		public void close() {
		}
	}

	private static class TestPlugin implements JadxPlugin {
		private final String pluginId;

		private TestPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}

	private static final class OtherTestPlugin extends TestPlugin {
		private OtherTestPlugin(String pluginId) {
			super(pluginId);
		}
	}
}
