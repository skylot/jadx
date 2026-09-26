package jadx.core.plugins;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.input.JadxCodeInput;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.api.plugins.pass.JadxPassInfo;
import jadx.api.plugins.pass.impl.SimpleJadxPassInfo;
import jadx.api.plugins.pass.types.JadxAfterLoadPass;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JadxPluginManagerTest {

	@TempDir
	Path tmp;

	@Test
	public void loadWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		pluginManager.load(new TestLoader(new TestPlugin("plugin-b"), new TestPlugin("plugin-a")));

		assertThat(pluginManager.getAllPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("plugin-a", "plugin-b");
		assertThat(pluginManager.getResolvedPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("plugin-a", "plugin-b");
		// plugin data is not a plugin context so it can't leak into plugins without decompiler
		assertThat(pluginManager.getAllPluginContexts()).allMatch(p -> !(p instanceof JadxPluginContext));
	}

	@Test
	public void disabledPluginsSkippedWithoutDecompiler() {
		JadxArgs args = new JadxArgs();
		args.getDisabledPlugins().add("disabled-plugin");
		JadxPluginManager pluginManager = new JadxPluginManager(args);
		pluginManager.load(new TestLoader(new TestPlugin("disabled-plugin"), new TestPlugin("enabled-plugin")));
		assertThat(pluginManager.register(new TestPlugin("disabled-plugin"))).isNull();

		assertThat(pluginManager.getAllPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("enabled-plugin");
	}

	@Test
	public void duplicatedPluginIdRejectedWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		assertThatThrownBy(() -> pluginManager.load(new TestLoader(new TestPlugin("same-id"), new OtherTestPlugin("same-id"))))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Duplicate plugin id");
	}

	@Test
	public void providesResolvedWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		pluginManager.providesSuggestion("input", "input-2");
		pluginManager.load(new TestLoader(
				new TestPlugin("input-1", "input"),
				new TestPlugin("input-2", "input"),
				new TestPlugin("other")));

		assertThat(pluginManager.getAllPluginContexts()).hasSize(3);
		assertThat(pluginManager.getResolvedPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("input-2", "other");
	}

	@Test
	public void addPluginListenersWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));
		List<String> added = new ArrayList<>();
		pluginManager.registerAddPluginListener(p -> added.add(p.getPluginId()));
		PluginContext registered = pluginManager.register(new TestPlugin("registered-plugin"));

		assertThat(added).containsExactly("loaded-plugin", "registered-plugin");
		assertThat(registered).isNotNull();
		assertThat(pluginManager.getResolvedPluginContexts()).contains(registered);
	}

	@Test
	public void registeredPluginsKeptOnLoadWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		pluginManager.register(new TestPlugin("registered-plugin"));
		pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));
		pluginManager.load(new TestLoader(new TestPlugin("loaded-plugin")));

		assertThat(pluginManager.getAllPluginContexts())
				.extracting(PluginContext::getPluginId)
				.containsExactly("loaded-plugin", "registered-plugin");
	}

	@Test
	public void optionsAppliedFromArgsWithoutDecompiler() {
		JadxArgs args = new JadxArgs();
		args.getPluginOptions().put(OptionsPlugin.OPTION, "custom");
		JadxPluginManager pluginManager = new JadxPluginManager(args);
		OptionsPlugin plugin = new OptionsPlugin();
		PluginContext context = pluginManager.register(plugin);
		assertThat(context).isNotNull();

		OptionsPlugin.Options options = plugin.buildOptions();
		context.registerOptions(options);
		assertThat(context.getOptions()).isSameAs(options);
		assertThat(options.value).isEqualTo("custom");
		assertThat(context.getInputsHash()).isNotEmpty();
	}

	@Test
	public void initNotAvailableWithoutDecompiler() {
		JadxPluginManager pluginManager = new JadxPluginManager(new JadxArgs());
		RecordingPlugin plugin = new RecordingPlugin("recording-plugin", ctx -> {
		});
		PluginContext context = pluginManager.register(plugin);
		assertThat(context).isNotNull();

		assertThatThrownBy(pluginManager::initAll).isInstanceOf(JadxRuntimeException.class);
		assertThat(plugin.initContext).isNull();
		assertThat(context.isInitialized()).isFalse();
	}

	@Test
	public void pluginInitBoundToDecompiler() {
		JadxArgs args = new JadxArgs();
		args.getPluginOptions().put(OptionsPlugin.OPTION, "custom");
		JadxCodeInput codeInput = (files) -> null;
		OptionsPlugin.Options options = new OptionsPlugin().buildOptions();
		// option names should start with plugin id
		RecordingPlugin plugin = new RecordingPlugin(OptionsPlugin.PLUGIN_ID, ctx -> {
			ctx.registerOptions(options);
			ctx.registerInputsHashSupplier(() -> "custom-hash");
			ctx.addCodeInput(codeInput);
		});
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			JadxPluginManager pluginManager = decompiler.getPluginManager();
			PluginContext pluginContext = pluginManager.register(plugin);
			assertThat(pluginContext).isNotNull();
			pluginManager.initResolved();

			JadxPluginContext ctx = plugin.initContext;
			assertThat(ctx).isNotNull();
			assertThat(ctx).isNotSameAs(pluginContext);
			assertThat(ctx.getDecompiler()).isSameAs(decompiler);
			assertThat(ctx.getArgs()).isSameAs(args);
			assertThat(ctx.events()).isSameAs(decompiler.events());
			assertThat(ctx.getResourcesLoader()).isSameAs(decompiler.getResourcesLoader());
			assertThat(ctx.getZipReader()).isSameAs(decompiler.getZipReader());
			assertThat(ctx.getGuiContext()).isNull();
			assertThat(ctx.plugins().getById(OptionsPlugin.PLUGIN_ID)).isSameAs(pluginContext);

			// data registered in plugin init stored in plugin data
			assertThat(pluginContext.isInitialized()).isTrue();
			assertThat(pluginContext.getOptions()).isSameAs(options);
			assertThat(options.value).isEqualTo("custom");
			assertThat(pluginContext.getInputsHash()).isEqualTo("custom-hash");
			assertThat(pluginContext.getCodeInputs()).containsExactly(codeInput);

			pluginManager.unloadResolved();
			assertThat(plugin.unloadCount).isEqualTo(1);
		}
	}

	@Test
	public void pluginPassesAddedToDecompiler() throws Exception {
		Path smali = tmp.resolve("HelloWorld.smali");
		Files.write(smali, (".class Lsmali/HelloWorld;\n"
				+ ".super Ljava/lang/Object;\n").getBytes(StandardCharsets.UTF_8));
		JadxArgs args = new JadxArgs();
		args.getInputFiles().add(smali.toFile());
		args.setOutDir(tmp.resolve("out").toFile());
		List<JadxDecompiler> afterLoad = new ArrayList<>();
		RecordingPlugin plugin = new RecordingPlugin("pass-plugin", ctx -> ctx.addPass(new TestAfterLoadPass(afterLoad)));
		try (JadxDecompiler decompiler = new JadxDecompiler(args)) {
			decompiler.registerPlugin(plugin);
			decompiler.load();
			assertThat(afterLoad).containsExactly(decompiler);
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
		private final @Nullable String provides;

		private TestPlugin(String pluginId) {
			this(pluginId, null);
		}

		private TestPlugin(String pluginId, @Nullable String provides) {
			this.pluginId = pluginId;
			this.provides = provides;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			JadxPluginInfoBuilder builder = JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test");
			if (provides != null) {
				builder.provides(provides);
			}
			return builder.build();
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

	private static final class RecordingPlugin extends TestPlugin {
		private final Consumer<JadxPluginContext> initAction;
		private @Nullable JadxPluginContext initContext;
		private int unloadCount;

		private RecordingPlugin(String pluginId, Consumer<JadxPluginContext> initAction) {
			super(pluginId);
			this.initAction = initAction;
		}

		@Override
		public void init(JadxPluginContext context) {
			initContext = context;
			initAction.accept(context);
		}

		@Override
		public void unload() {
			unloadCount++;
		}
	}

	private static final class OptionsPlugin extends TestPlugin {
		private static final String PLUGIN_ID = "options-plugin";
		private static final String OPTION = PLUGIN_ID + ".value";

		private OptionsPlugin() {
			super(PLUGIN_ID);
		}

		Options buildOptions() {
			return new Options();
		}

		private static final class Options extends BasePluginOptionsBuilder {
			private String value;

			@Override
			public void registerOptions() {
				strOption(OPTION)
						.description("test option")
						.defaultValue("default")
						.setter(v -> value = v);
			}
		}
	}

	private static final class TestAfterLoadPass implements JadxAfterLoadPass {
		private final List<JadxDecompiler> afterLoad;

		private TestAfterLoadPass(List<JadxDecompiler> afterLoad) {
			this.afterLoad = afterLoad;
		}

		@Override
		public JadxPassInfo getInfo() {
			return new SimpleJadxPassInfo("TestAfterLoadPass");
		}

		@Override
		public void init(JadxDecompiler decompiler) {
			afterLoad.add(decompiler);
		}
	}
}
