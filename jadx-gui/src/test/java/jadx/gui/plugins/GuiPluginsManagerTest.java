package jadx.gui.plugins;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.gui.plugins.JadxGlobalGuiPlugin;
import jadx.api.gui.plugins.JadxGuiContextExt;
import jadx.api.plugins.JadxPlugin;
import jadx.api.plugins.JadxPluginContext;
import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.JadxPluginInfoBuilder;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.gui.ISettingsGroup;
import jadx.api.plugins.loader.JadxPluginLoader;
import jadx.api.plugins.options.JadxPluginOptions;
import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;
import jadx.core.plugins.JadxPluginManager;
import jadx.core.plugins.PluginRuntime;
import jadx.gui.plugins.context.GuiPluginContext;
import jadx.gui.plugins.context.TestMainWindowShim;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.plugins.CollectPlugins;
import jadx.plugins.tools.JadxExternalPluginsLoader;
import jadx.plugins.tools.JadxPluginsTools;
import jadx.plugins.tools.data.JadxPluginListEntry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class GuiPluginsManagerTest {

	@Test
	public void globalPluginsLoadFailureDontBreakProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		TestPlugin globalPlugin = new TestPlugin("bad-global-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new RuntimeException("test");
			}
		};
		assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();
		manager.load();

		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			assertThatCode(() -> loadProjectPlugins(manager, projectDecompiler)).doesNotThrowAnyException();
		}
		assertThatCode(manager::getGlobalPlugins).doesNotThrowAnyException();
		assertThat(manager.getGlobalPlugins().stream().filter(PluginRuntime::isInitialized)).isEmpty();
		assertThatCode(manager::runGlobalUnload).doesNotThrowAnyException();
	}

	@Test
	public void failedGlobalPluginNotInjectedIntoProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingGlobalPlugin failedPlugin = new CountingGlobalPlugin("failed-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new RuntimeException("test");
			}
		};
		CountingGlobalPlugin goodPlugin = new CountingGlobalPlugin("good-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(failedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(goodPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("good-plugin");
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			loadProjectPlugins(manager, projectDecompiler);
			projectDecompiler.getPluginManager().initResolved(projectDecompiler);
			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("good-plugin");
		}
		manager.runGlobalUnload();

		assertThat(failedPlugin.projectInitCount).isZero();
		assertThat(failedPlugin.globalUnloadCount).isZero();
		assertThat(goodPlugin.projectInitCount).isEqualTo(1);
		assertThat(goodPlugin.globalUnloadCount).isEqualTo(1);
	}

	@Test
	public void globalInitErrorDontStopOtherPlugins() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountingGlobalPlugin failedPlugin = new CountingGlobalPlugin("a-failed-plugin") {
			@Override
			public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
				throw new NoClassDefFoundError("test");
			}
		};
		CountingGlobalPlugin goodPlugin = new CountingGlobalPlugin("b-good-plugin");
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(failedPlugin)).isNotNull();
		assertThat(manager.getGlobalPluginManager().register(goodPlugin)).isNotNull();

		assertThatCode(() -> manager.runGlobalInit(manager.getGlobalPlugins())).doesNotThrowAnyException();
		assertThat(goodPlugin.globalInitCount).isEqualTo(1);
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactly("b-good-plugin");
	}

	@Test
	public void loadedGlobalPluginsInjectedIntoProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();

			assertThat(manager.getGlobalPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("global-plugin");

			loadProjectPlugins(manager, projectDecompiler);

			assertThat(projectDecompiler.getPluginManager().getAllPlugins())
					.extracting(PluginRuntime::getPluginId)
					.containsExactly("global-plugin");

			// same plugin instance is used in both scopes
			PluginRuntime projectPlugin = projectDecompiler.getPluginManager().getAllPlugins().first();
			assertThat(projectPlugin.getPluginInstance()).isSameAs(globalPlugin);
		}
	}

	@Test
	public void globalPluginCustomSettingsUsedInProjectScope() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			TestPlugin globalPlugin = new TestPlugin("global-plugin");
			manager.initGuiPluginsContextForGlobalScope();
			PluginRuntime globalPluginRuntime = manager.getGlobalPluginManager().register(globalPlugin);
			assertThat(globalPluginRuntime).isNotNull();
			GuiPluginContext globalGuiContext = (GuiPluginContext) globalPluginRuntime.getAppContext().getGuiContext();
			ISettingsGroup settingsGroup = new TestSettingsGroup();
			globalGuiContext.settings().setCustomSettingsGroup(settingsGroup);

			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			loadProjectPlugins(manager, projectDecompiler);

			PluginRuntime projectPlugin = projectDecompiler.getPluginManager().getAllPlugins().first();
			GuiPluginContext projectGuiContext = (GuiPluginContext) projectPlugin.getAppContext().getGuiContext();
			assertThat(projectGuiContext.getCustomSettingsGroup()).isSameAs(settingsGroup);
		}
	}

	@Test
	public void projectPluginCustomSettingsNotAffected() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		JadxArgs jadxArgs = new JadxArgs();
		JadxPluginManager globalPluginManager = new JadxPluginManager(jadxArgs);
		try (JadxDecompiler projectDecompiler = new JadxDecompiler()) {
			manager.initGuiPluginsContext(globalPluginManager, jadxArgs, true);
			assertThat(globalPluginManager.register(new TestPlugin("global-plugin"))).isNotNull();

			manager.initGuiPluginsContext(projectDecompiler.getPluginManager(), projectDecompiler.getArgs(), false);
			PluginRuntime projectOnly = projectDecompiler.getPluginManager().register(new TestPlugin("project-plugin"));
			assertThat(projectOnly).isNotNull();
			GuiPluginContext projectOnlyGui = (GuiPluginContext) projectOnly.getAppContext().getGuiContext();
			ISettingsGroup ownGroup = new TestSettingsGroup();
			projectOnlyGui.settings().setCustomSettingsGroup(ownGroup);

			loadProjectPlugins(manager, projectDecompiler);

			assertThat(projectOnlyGui.getCustomSettingsGroup()).isSameAs(ownGroup);
		}
	}

	@Test
	public void globalPluginsCollectedForSettingsWithoutProject() {
		MainWindow mainWindow = TestMainWindowShim.build();
		JadxPlugin projectPlugin = new TestProjectPlugin("project-plugin");
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow) {
			@Override
			public JadxPluginLoader buildProjectPluginLoader() {
				return new TestPluginLoader(projectPlugin);
			}
		};
		TestMainWindowShim.setPluginsManager(mainWindow, manager);

		TestPlugin globalPlugin = new TestPlugin("global-plugin") {
			@Override
			public JadxPluginOptions buildOptions() {
				return new TestOptions("global-plugin");
			}
		};
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());

		List<PluginRuntime> plugins = new CollectPlugins(mainWindow).build();
		assertThat(plugins)
				.extracting(PluginRuntime::getPluginId)
				.containsExactlyInAnyOrder("global-plugin", "project-plugin");
		assertThat(plugins)
				.filteredOn(p -> p.getPluginId().equals("global-plugin"))
				.allSatisfy(p -> assertThat(p.getOptions()).isNotNull());
	}

	@Test
	public void settingsWindowReloadedAfterGlobalInit() throws InterruptedException {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		CountDownLatch reloaded = new CountDownLatch(1);
		mainWindow.events().global().addListener(JadxEvents.RELOAD_SETTINGS_WINDOW, ev -> reloaded.countDown());

		assertThat(manager.getGlobalPluginManager().register(new TestPlugin("global-plugin"))).isNotNull();
		manager.load();

		assertThat(reloaded.await(10, TimeUnit.SECONDS)).isTrue();
	}

	@Test
	public void globalPluginOptionsUpdated() {
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);
		OptionsGlobalPlugin plugin = new OptionsGlobalPlugin();
		manager.initGuiPluginsContextForGlobalScope();
		assertThat(manager.getGlobalPluginManager().register(plugin)).isNotNull();
		manager.runGlobalInit(manager.getGlobalPlugins());
		assertThat(plugin.value).isEqualTo("default");

		mainWindow.getSettings().getPluginOptions().put("options-plugin.value", "changed");
		manager.updateGlobalPluginsOptions();
		assertThat(plugin.value).isEqualTo("changed");
	}

	private static final class OptionsGlobalPlugin extends JadxGlobalGuiPlugin {
		private String value;

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId("options-plugin").name("options-plugin").description("test").build();
		}

		@Override
		public JadxPluginOptions buildOptions() {
			return new BasePluginOptionsBuilder() {
				@Override
				public void registerOptions() {
					strOption("options-plugin.value")
							.description("test option")
							.defaultValue("default")
							.setter(v -> value = v);
				}
			};
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
		}
	}

	private static final class TestProjectPlugin implements JadxPlugin {
		private final String pluginId;

		private TestProjectPlugin(String pluginId) {
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

	private static final class TestOptions extends BasePluginOptionsBuilder {
		private final String pluginId;

		private TestOptions(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public void registerOptions() {
			strOption(pluginId + ".test")
					.description("test option")
					.defaultValue("")
					.setter(v -> {
					});
		}
	}

	@Test
	public void projectPluginLoaderExposesGlobalPluginsAsParents(@TempDir Path tempDir) throws Exception {
		Path childJar = compileChildProjectPluginJar(tempDir, "proj-child", "global-parent");
		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow);

		try (JadxExternalPluginsLoader loaderBefore = (JadxExternalPluginsLoader) manager.buildProjectPluginLoader()) {
			assertThat(loaderBefore.loadPluginsFromPath(childJar)).isEmpty();
		}

		TestPlugin globalPlugin = new TestPlugin("global-parent");
		assertThat(manager.getGlobalPluginManager().register(globalPlugin)).isNotNull();

		try (JadxExternalPluginsLoader projectLoader = (JadxExternalPluginsLoader) manager.buildProjectPluginLoader()) {
			List<JadxPlugin> loaded = projectLoader.loadPluginsFromPath(childJar);
			assertThat(loaded).hasSize(1);
			JadxPlugin childPlugin = loaded.get(0);
			assertThat(childPlugin.getClass().getClassLoader().getParent())
					.isSameAs(globalPlugin.getClass().getClassLoader());
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void scheduledGlobalChangesOrderAndCascade(@TempDir Path tempDir) throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path parentJar = compilePluginJar(
				tempDir,
				jarsDir.resolve("z-parent-global.jar"),
				"pglob.ParentGlobalPlugin",
				Map.of(
						"pglob/GlobalApi.java",
						"package pglob;\n"
								+ "import java.util.ArrayList;\n"
								+ "import java.util.List;\n"
								+ "public class GlobalApi {\n"
								+ "    private static final List<String> EVENTS = new ArrayList<>();\n"
								+ "    public static void record(String e) { EVENTS.add(e); }\n"
								+ "    public static List<String> getEvents() { return EVENTS; }\n"
								+ "}\n",
						"pglob/ParentGlobalPlugin.java",
						"package pglob;\n"
								+ "import jadx.api.gui.plugins.JadxGlobalGuiPlugin;\n"
								+ "import jadx.api.gui.plugins.JadxGuiContextExt;\n"
								+ "import jadx.api.plugins.JadxPluginContext;\n"
								+ "import jadx.api.plugins.JadxPluginInfo;\n"
								+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
								+ "import org.jetbrains.annotations.NotNull;\n"
								+ "public class ParentGlobalPlugin extends JadxGlobalGuiPlugin {\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"z-parent-global\").name(\"P\").description(\"d\").build();\n"
								+ "    }\n"
								+ "    @Override public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {\n"
								+ "        GlobalApi.record(\"parent-global-init\");\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {}\n"
								+ "    @Override public void globalUnload() {\n"
								+ "        GlobalApi.record(\"parent-global-unload\");\n"
								+ "    }\n"
								+ "}\n"),
				new String[] {});
		Path childJar = compilePluginJar(
				tempDir,
				jarsDir.resolve("a-child-global.jar"),
				"cglob.ChildGlobalPlugin",
				Map.of(
						"cglob/ChildGlobalPlugin.java",
						"package cglob;\n"
								+ "import jadx.api.gui.plugins.JadxGlobalGuiPlugin;\n"
								+ "import jadx.api.gui.plugins.JadxGuiContextExt;\n"
								+ "import jadx.api.plugins.JadxPluginContext;\n"
								+ "import jadx.api.plugins.JadxPluginInfo;\n"
								+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
								+ "import org.jetbrains.annotations.NotNull;\n"
								+ "import pglob.GlobalApi;\n"
								+ "public class ChildGlobalPlugin extends JadxGlobalGuiPlugin {\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"a-child-global\").name(\"C\").description(\"d\")\n"
								+ "                .parentPlugin(\"z-parent-global\").build();\n"
								+ "    }\n"
								+ "    @Override public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {\n"
								+ "        GlobalApi.record(\"child-global-init\");\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {}\n"
								+ "    @Override public void globalUnload() {\n"
								+ "        GlobalApi.record(\"child-global-unload\");\n"
								+ "    }\n"
								+ "}\n"),
				new String[] { parentJar.toString() });

		JadxPluginListEntry parentEntry = new JadxPluginListEntry();
		parentEntry.setPluginId("z-parent-global");
		parentEntry.setLocationId("file:" + parentJar);
		JadxPluginsTools tools = new JadxPluginsTools(
				tempDir.resolve("plugins.json"),
				tempDir.resolve("installed"),
				tempDir.resolve("dropins"),
				() -> List.of(parentEntry));
		tools.installPlugins("file:" + childJar);

		MainWindow mainWindow = TestMainWindowShim.build();
		GuiPluginsManager manager = new GuiPluginsManager(mainWindow, tools);
		manager.initGuiPluginsContextForGlobalScope();

		// Intentionally schedule child before parent: manager must load parent first.
		manager.scheduleGlobalLoad("a-child-global");
		manager.scheduleGlobalLoad("z-parent-global");
		manager.runScheduledGlobalChanges();

		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactlyInAnyOrder("z-parent-global", "a-child-global");

		ClassLoader parentCl1 = findGlobalRuntime(manager, "z-parent-global").getPluginInstance().getClass().getClassLoader();
		ClassLoader childCl1 = findGlobalRuntime(manager, "a-child-global").getPluginInstance().getClass().getClassLoader();
		assertThat(childCl1.getParent()).isSameAs(parentCl1);
		List<String> events1 = (List<String>) parentCl1.loadClass("pglob.GlobalApi").getMethod("getEvents").invoke(null);
		assertThat(events1).containsExactly("parent-global-init", "child-global-init");

		// Reload ONLY parent: child must cascade-unload before parent, then reload onto new parent CL.
		manager.scheduleGlobalUnload("z-parent-global");
		manager.scheduleGlobalLoad("z-parent-global");
		manager.runScheduledGlobalChanges();

		assertThat(events1).containsExactly(
				"parent-global-init", "child-global-init", "child-global-unload", "parent-global-unload");
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactlyInAnyOrder("z-parent-global", "a-child-global");
		ClassLoader parentCl2 = findGlobalRuntime(manager, "z-parent-global").getPluginInstance().getClass().getClassLoader();
		ClassLoader childCl2 = findGlobalRuntime(manager, "a-child-global").getPluginInstance().getClass().getClassLoader();
		assertThat(parentCl2).isNotSameAs(parentCl1);
		assertThat(childCl2).isNotSameAs(childCl1);
		assertThat(childCl2.getParent()).isSameAs(parentCl2);

		// Disable & unload ONLY parent: child must cascade-unload first.
		tools.changeDisabledStatus("z-parent-global", true);
		manager.scheduleGlobalUnload("z-parent-global");
		manager.runScheduledGlobalChanges();
		assertThat(manager.getGlobalPlugins()).isEmpty();
		List<String> events2 = (List<String>) parentCl2.loadClass("pglob.GlobalApi").getMethod("getEvents").invoke(null);
		assertThat(events2).containsExactly(
				"parent-global-init", "child-global-init", "child-global-unload", "parent-global-unload");

		// Re-enable & load ONLY parent: child (still enabled in plugins.json) must reload after parent.
		tools.changeDisabledStatus("z-parent-global", false);
		manager.scheduleGlobalLoad("z-parent-global");
		manager.runScheduledGlobalChanges();
		assertThat(manager.getGlobalPlugins())
				.extracting(PluginRuntime::getPluginId)
				.containsExactlyInAnyOrder("z-parent-global", "a-child-global");

		// Cleanly unload everything so temp jars are released
		manager.scheduleGlobalUnload("z-parent-global");
		manager.runScheduledGlobalChanges();
		assertThat(manager.getGlobalPlugins()).isEmpty();
	}

	@Test
	public void projectPluginInheritsBundledDexInputPlugin(@TempDir Path tempDir) throws Exception {
		Path dropinsDir = tempDir.resolve("dropins");
		Files.createDirectories(dropinsDir);
		compilePluginJar(
				tempDir,
				dropinsDir.resolve("dex-child.jar"),
				"dexchild.DexChildPlugin",
				Map.of(
						"dexchild/DexChildPlugin.java",
						"package dexchild;\n"
								+ "import jadx.api.plugins.JadxPlugin;\n"
								+ "import jadx.api.plugins.JadxPluginContext;\n"
								+ "import jadx.api.plugins.JadxPluginInfo;\n"
								+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
								+ "import jadx.plugins.input.dex.DexInputPlugin;\n"
								+ "public class DexChildPlugin implements JadxPlugin {\n"
								+ "    private boolean verified;\n"
								+ "    public boolean isVerified() { return verified; }\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"dex-child\")\n"
								+ "                .name(\"Dex Child\").description(\"test\")\n"
								+ "                .parentPlugin(\"dex-input\").build();\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {\n"
								+ "        DexInputPlugin dex = context.plugins().getInstance(DexInputPlugin.class);\n"
								+ "        verified = dex != null && DexInputPlugin.PLUGIN_ID.equals(dex.getPluginInfo().getPluginId());\n"
								+ "    }\n"
								+ "}\n"),
				new String[] {});

		JadxPluginsTools tools = new JadxPluginsTools(
				tempDir.resolve("plugins.json"),
				tempDir.resolve("installed"),
				dropinsDir,
				Collections::emptyList);
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(
				cls -> !JadxGlobalGuiPlugin.class.isAssignableFrom(cls), tools);
				JadxDecompiler decompiler = new JadxDecompiler()) {
			decompiler.getPluginManager().load(loader);
			SortedSet<PluginRuntime> resolved = decompiler.getPluginManager().getResolvedPlugins();
			decompiler.getPluginManager().init(decompiler, resolved);

			PluginRuntime dexRt = resolved.stream()
					.filter(p -> "dex-input".equals(p.getPluginId()))
					.findFirst()
					.orElseThrow();
			PluginRuntime childRt = resolved.stream()
					.filter(p -> "dex-child".equals(p.getPluginId()))
					.findFirst()
					.orElseThrow();
			JadxPlugin instance = childRt.getPluginInstance();
			assertThat(instance.getClass().getClassLoader().getParent())
					.isSameAs(dexRt.getPluginInstance().getClass().getClassLoader());
			assertThat(instance.getClass().getMethod("isVerified").invoke(instance)).isEqualTo(true);
		}
	}

	private static PluginRuntime findGlobalRuntime(GuiPluginsManager manager, String pluginId) {
		return manager.getGlobalPlugins().stream()
				.filter(p -> pluginId.equals(p.getPluginId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Global plugin not found: " + pluginId));
	}

	private static Path compileChildProjectPluginJar(Path tempDir, String pluginId, String parentPlugin) throws IOException {
		String src = "package projchild;\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "public class ChildProjectPlugin implements JadxPlugin {\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return JadxPluginInfoBuilder.pluginId(\"" + pluginId + "\")\n"
				+ "                .name(\"Child Project Plugin\")\n"
				+ "                .description(\"test\")\n"
				+ "                .parentPlugin(\"" + parentPlugin + "\")\n"
				+ "                .build();\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "    }\n"
				+ "}\n";
		return compilePluginJar(
				tempDir,
				tempDir.resolve("proj-child.jar"),
				"projchild.ChildProjectPlugin",
				Map.of("projchild/ChildProjectPlugin.java", src),
				new String[] {});
	}

	private static Path compilePluginJar(
			Path tempDir,
			Path jarPath,
			String serviceImplClass,
			Map<String, String> sourceFiles,
			String[] extraCp) throws IOException {
		Path workDir = Files.createTempDirectory(tempDir, "compile");
		Path srcDir = workDir.resolve("src");
		Path classesDir = workDir.resolve("classes");
		Files.createDirectories(classesDir);

		List<String> srcPaths = new ArrayList<>();
		for (Map.Entry<String, String> entry : sourceFiles.entrySet()) {
			Path srcFile = srcDir.resolve(entry.getKey());
			Files.createDirectories(srcFile.getParent());
			Files.writeString(srcFile, entry.getValue(), StandardCharsets.UTF_8);
			srcPaths.add(srcFile.toString());
		}

		StringBuilder cp = new StringBuilder(System.getProperty("java.class.path"));
		for (String entry : extraCp) {
			cp.append(File.pathSeparator).append(entry);
		}

		List<String> args = new ArrayList<>();
		args.add("-cp");
		args.add(cp.toString());
		args.add("-d");
		args.add(classesDir.toString());
		args.addAll(srcPaths);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int result = compiler.run(null, null, null, args.toArray(String[]::new));
		assertThat(result).isZero();

		Files.createDirectories(jarPath.getParent());
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jarPath));
				Stream<Path> walk = Files.walk(classesDir)) {
			for (Path classFile : walk.filter(Files::isRegularFile).collect(Collectors.toList())) {
				String entryName = classesDir.relativize(classFile).toString().replace('\\', '/');
				out.putNextEntry(new JarEntry(entryName));
				out.write(Files.readAllBytes(classFile));
				out.closeEntry();
			}
			out.putNextEntry(new JarEntry("META-INF/services/jadx.api.plugins.JadxPlugin"));
			out.write((serviceImplClass + "\n").getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		return jarPath;
	}

	private static void loadProjectPlugins(GuiPluginsManager manager, JadxDecompiler projectDecompiler) {
		projectDecompiler.getPluginManager().load(new GuiPluginsLoader(manager.getGlobalPlugins(), new TestPluginLoader()));
	}

	private static final class TestSettingsGroup implements ISettingsGroup {
		@Override
		public String getTitle() {
			return "custom";
		}

		@Override
		public JComponent buildComponent() {
			return new JLabel();
		}

		@Override
		public List<ISettingsGroup> getSubGroups() {
			return Collections.emptyList();
		}
	}

	private static class CountingGlobalPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;
		int globalInitCount;
		int projectInitCount;
		int globalUnloadCount;

		private CountingGlobalPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
			globalInitCount++;
		}

		@Override
		public void init(JadxPluginContext context, @NotNull JadxGuiContextExt guiContextExt) {
			projectInitCount++;
		}

		@Override
		public void globalUnload() {
			globalUnloadCount++;
		}
	}

	private static class TestPlugin extends JadxGlobalGuiPlugin {
		private final String pluginId;

		private TestPlugin(String pluginId) {
			this.pluginId = pluginId;
		}

		@Override
		public JadxPluginInfo getPluginInfo() {
			return JadxPluginInfoBuilder.pluginId(pluginId).name(pluginId).description("test").build();
		}

		@Override
		public void pluginGlobalInit(@NotNull JadxGuiContextExt guiContext) {
		}

		@Override
		public void init(JadxPluginContext context) {
		}
	}
}
