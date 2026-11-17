package jadx.core.plugins;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

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
			assertThat(pluginManager.getAllPlugins()).hasSize(1);

			assertThatCode(() -> pluginManager.load(loader)).doesNotThrowAnyException();
			assertThat(pluginManager.getAllPlugins()).hasSize(1);
		}
	}

	// Plugins added with 'register' method (i.e global gui plugins) should survive plugins load
	@Test
	public void loadKeepRegisteredPlugins() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			assertThat(pluginManager.register(new TestPlugin("registered-plugin"))).isNotNull();

			pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));

			assertThat(pluginManager.getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactlyInAnyOrder("registered-plugin", "loaded-plugin");

			// and still kept after a repeated load
			pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));
			assertThat(pluginManager.getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
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
			int pluginsCount = decompiler.getPluginManager().getAllPlugins().size();
			assertThat(pluginsCount).isPositive();

			assertThatCode(decompiler::reloadPasses).doesNotThrowAnyException();
			assertThat(decompiler.getPluginManager().getAllPlugins()).hasSize(pluginsCount);

			assertThatCode(decompiler::reloadPasses).doesNotThrowAnyException();
			assertThat(decompiler.getPluginManager().getAllPlugins()).hasSize(pluginsCount);
		}
	}

	@Test
	public void dependencyAndParentInitUnloadOrder() {
		List<String> initOrder = new ArrayList<>();
		List<String> unloadOrder = new ArrayList<>();

		JadxPlugin grandParent = new OrderedPlugin("z-base", null, List.of(), initOrder, unloadOrder);
		JadxPlugin parent = new OrderedPlugin("m-parent", "z-base", List.of(), initOrder, unloadOrder);
		JadxPlugin extraDep = new OrderedPlugin("x-helper", null, List.of("z-base"), initOrder, unloadOrder);
		JadxPlugin child = new OrderedPlugin("a-child", "m-parent", List.of("x-helper"), initOrder, unloadOrder);

		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			pluginManager.load(new TestLoader(child, parent, grandParent, extraDep));
			pluginManager.initResolved(decompiler);

			assertThat(pluginManager.getResolvedPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("a-child", "m-parent", "x-helper", "z-base");
			assertThat(initOrder).containsExactly("z-base", "m-parent", "x-helper", "a-child");

			pluginManager.unloadResolved();
			assertThat(unloadOrder).containsExactly("a-child", "x-helper", "m-parent", "z-base");
		}
	}

	@Test
	public void disabledParentPrunesDependentsTransitively() {
		JadxArgs args = new JadxArgs();
		args.getDisabledPlugins().add("base");
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			JadxPlugin base = new OrderedPlugin("base", null, List.of(), null, null);
			JadxPlugin child = new OrderedPlugin("child", "base", List.of(), null, null);
			JadxPlugin grandChild = new OrderedPlugin("grand-child", null, List.of("child"), null, null);
			JadxPlugin independent = new TestPlugin("independent");

			pluginManager.load(new TestLoader(base, child, grandChild, independent));
			assertThat(pluginManager.getResolvedPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("independent");
		}
	}

	@Test
	public void cyclicDependenciesRejected() {
		try (JadxDecompiler decompiler = new JadxDecompiler()) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			JadxPlugin p1 = new OrderedPlugin("p1", null, List.of("p2"), null, null);
			JadxPlugin p2 = new OrderedPlugin("p2", "p1", List.of(), null, null);
			assertThatThrownBy(() -> pluginManager.load(new TestLoader(p1, p2)))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("Cyclic plugin dependency detected");
		}
	}

	@Test
	public void selfDependencyAndParentRejectedInBuilder() {
		assertThatThrownBy(() -> JadxPluginInfoBuilder.pluginId("p1")
				.name("P1")
				.description("test")
				.parentPlugin("p1")
				.build())
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("own parent");

		assertThatThrownBy(() -> JadxPluginInfoBuilder.pluginId("p1")
				.name("P1")
				.description("test")
				.dependencyPlugin("p1")
				.build())
						.isInstanceOf(IllegalArgumentException.class)
						.hasMessageContaining("depend on itself");
	}

	private static final class OrderedPlugin implements JadxPlugin {
		private final String pluginId;
		private final String parentPlugin;
		private final List<String> deps;
		private final Consumer<String> onInit;
		private final Consumer<String> onUnload;

		private OrderedPlugin(String pluginId, String parentPlugin, List<String> deps,
				List<String> initOrder, List<String> unloadOrder) {
			this.pluginId = pluginId;
			this.parentPlugin = parentPlugin;
			this.deps = deps;
			this.onInit = initOrder != null ? initOrder::add : id -> {
			};
			this.onUnload = unloadOrder != null ? unloadOrder::add : id -> {
			};
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId)
					.name(pluginId)
					.description("test")
					.parentPlugin(parentPlugin)
					.dependencyPlugins(deps)
					.build();
		}

		@Override
		public void init(JadxPluginContext context) {
			onInit.accept(pluginId);
		}

		@Override
		public void unload() {
			onUnload.accept(pluginId);
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
