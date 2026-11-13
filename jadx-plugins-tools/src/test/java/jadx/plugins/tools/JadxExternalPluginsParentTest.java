package jadx.plugins.tools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxDecompiler;
import jadx.api.plugins.JadxPlugin;
import jadx.core.plugins.PluginRuntime;
import jadx.core.utils.exceptions.JadxRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxExternalPluginsParentTest {

	@TempDir
	Path tempDir;

	@Test
	void parentAndChildOutOfOrderInDropins() throws Exception {
		Path dropinsDir = tempDir.resolve("dropins");
		Files.createDirectories(dropinsDir);

		Path parentJar = buildParentPluginJar(dropinsDir, "z-parent.jar", "z-parent");
		buildChildPluginJar(dropinsDir, "a-child.jar", "a-child", "z-parent", parentJar);

		JadxPluginsTools tools = newTools(dropinsDir);
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools);
				JadxDecompiler decompiler = new JadxDecompiler()) {
			decompiler.getPluginManager().load(loader);
			SortedSet<PluginRuntime> resolved = decompiler.getPluginManager().getResolvedPlugins();
			decompiler.getPluginManager().init(decompiler, resolved);

			PluginRuntime parentRt = findPlugin(resolved, "z-parent");
			PluginRuntime childRt = findPlugin(resolved, "a-child");
			assertThat(childRt.getPluginInstance().getClass().getClassLoader().getParent())
					.isSameAs(parentRt.getPluginInstance().getClass().getClassLoader());

			decompiler.getPluginManager().unload(resolved);
			List<String> events = readEvents(parentRt.getPluginInstance());
			assertThat(events).containsExactly(
					"parent-init",
					"child-init:hello-a-child",
					"child-unload",
					"parent-unload");
		}
	}

	@Test
	void multiLevelParentChainFromInstalledMetadata() throws Exception {
		Path buildDir = tempDir.resolve("build");
		Files.createDirectories(buildDir);

		Path parentJar = buildParentPluginJar(buildDir, "z-parent.jar", "z-parent");
		Path childJar = buildChildPluginJar(buildDir, "m-child.jar", "m-child", "z-parent", parentJar);
		Path grandChildJar = buildGrandChildPluginJar(
				buildDir, "a-grandchild.jar", "a-grandchild", "m-child", parentJar, childJar);

		JadxPluginsTools tools = newTools(tempDir.resolve("dropins"));
		// Install in reverse dependency order (parent & child resolved via plugins list, or directly first
		// and reverse metadata order in plugins.json).
		tools.install("file:" + parentJar);
		tools.install("file:" + childJar);
		tools.install("file:" + grandChildJar);

		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools);
				JadxDecompiler decompiler = new JadxDecompiler()) {
			decompiler.getPluginManager().load(loader);
			SortedSet<PluginRuntime> resolved = decompiler.getPluginManager().getResolvedPlugins();
			decompiler.getPluginManager().init(decompiler, resolved);

			PluginRuntime parentRt = findPlugin(resolved, "z-parent");
			PluginRuntime childRt = findPlugin(resolved, "m-child");
			PluginRuntime grandChildRt = findPlugin(resolved, "a-grandchild");

			ClassLoader parentCl = parentRt.getPluginInstance().getClass().getClassLoader();
			ClassLoader childCl = childRt.getPluginInstance().getClass().getClassLoader();
			ClassLoader grandChildCl = grandChildRt.getPluginInstance().getClass().getClassLoader();
			assertThat(childCl.getParent()).isSameAs(parentCl);
			assertThat(grandChildCl.getParent()).isSameAs(childCl);

			decompiler.getPluginManager().unload(resolved);
			List<String> events = readEvents(parentRt.getPluginInstance());
			assertThat(events).containsExactly(
					"parent-init",
					"child-init:hello-m-child",
					"grandchild-init:hello-a-grandchild:hello-m-child",
					"grandchild-unload",
					"child-unload",
					"parent-unload");
		}
	}

	@Test
	void externalParentPluginsInheritedByLoader() throws Exception {
		Path globalDropins = tempDir.resolve("global-dropins");
		Path projectDropins = tempDir.resolve("project-dropins");
		Files.createDirectories(globalDropins);
		Files.createDirectories(projectDropins);

		Path parentJar = buildParentPluginJar(globalDropins, "z-parent.jar", "z-parent");
		buildChildPluginJar(projectDropins, "a-child.jar", "a-child", "z-parent", parentJar);

		JadxPluginsTools globalTools = new JadxPluginsTools(
				tempDir.resolve("global-plugins.json"),
				tempDir.resolve("global-installed"),
				globalDropins,
				Collections::emptyList);
		JadxPluginsTools projectTools = new JadxPluginsTools(
				tempDir.resolve("project-plugins.json"),
				tempDir.resolve("project-installed"),
				projectDropins,
				Collections::emptyList);

		try (JadxExternalPluginsLoader globalLoader = new JadxExternalPluginsLoader(c -> true, globalTools)) {
			List<JadxPlugin> globalPlugins = globalLoader.load();
			assertThat(globalPlugins).hasSize(1);
			JadxPlugin parentPlugin = globalPlugins.get(0);

			try (JadxExternalPluginsLoader projectLoader = new JadxExternalPluginsLoader(c -> true, projectTools)) {
				projectLoader.addExternalPlugins(globalPlugins);
				List<JadxPlugin> projectPlugins = projectLoader.load();
				assertThat(projectPlugins).hasSize(1);
				JadxPlugin childPlugin = projectPlugins.get(0);
				assertThat(childPlugin.getClass().getClassLoader().getParent())
						.isSameAs(parentPlugin.getClass().getClassLoader());
			}
			// Closing projectLoader must not close the external parent class loader.
			assertThat(parentPlugin.getClass().getClassLoader().loadClass("p0parent.ParentApi")).isNotNull();
		}
	}

	@Test
	void loadPluginsFromPathInheritsLoadedParentAndSkipsOrphan() throws Exception {
		Path dropinsDir = tempDir.resolve("dropins");
		Path hotDir = tempDir.resolve("hot");
		Files.createDirectories(dropinsDir);
		Files.createDirectories(hotDir);

		Path parentJar = buildParentPluginJar(dropinsDir, "z-parent.jar", "z-parent");
		Path childJar = buildChildPluginJar(hotDir, "a-child.jar", "a-child", "z-parent", parentJar);
		Path orphanChildJar = buildChildPluginJar(hotDir, "orphan.jar", "orphan", "missing-parent", parentJar);

		JadxPluginsTools tools = newTools(dropinsDir);
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools)) {
			List<JadxPlugin> initial = loader.load();
			assertThat(initial).hasSize(1);
			ClassLoader parentCl = initial.get(0).getClass().getClassLoader();

			List<JadxPlugin> hotLoaded = loader.loadPluginsFromPath(childJar);
			assertThat(hotLoaded).hasSize(1);
			assertThat(hotLoaded.get(0).getClass().getClassLoader().getParent()).isSameAs(parentCl);

			assertThat(loader.loadPluginsFromPath(orphanChildJar)).isEmpty();
		}
	}

	@Test
	void missingOrDisabledParentSkipsChildGracefully() throws Exception {
		Path buildDir = tempDir.resolve("build");
		Files.createDirectories(buildDir);
		Path dropinsDir = tempDir.resolve("dropins");
		Files.createDirectories(dropinsDir);

		Path parentJar = buildParentPluginJar(buildDir, "z-parent.jar", "z-parent");
		buildChildPluginJar(dropinsDir, "missing-child.jar", "missing-child", "missing-parent", parentJar);

		JadxPluginsTools tools = newTools(dropinsDir);
		tools.install("file:" + parentJar);

		Path disabledChildJar = buildChildPluginJar(buildDir, "disabled-child.jar", "disabled-child", "z-parent", parentJar);
		tools.install("file:" + disabledChildJar);
		assertThat(tools.changeDisabledStatus("z-parent", true)).isTrue();

		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools)) {
			List<JadxPlugin> loaded = loader.load();
			assertThat(loaded).isEmpty();
		}
	}

	@Test
	void cyclicParentPluginsThrowException() throws Exception {
		Path dropinsDir = tempDir.resolve("dropins");
		Files.createDirectories(dropinsDir);

		buildSimpleParentingJar(dropinsDir, "cycle-a.jar", "cyclea", "CycleAPlugin", "cycle-a", "cycle-b");
		buildSimpleParentingJar(dropinsDir, "cycle-b.jar", "cycleb", "CycleBPlugin", "cycle-b", "cycle-a");

		JadxPluginsTools tools = newTools(dropinsDir);
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools)) {
			assertThatThrownBy(loader::load)
					.isInstanceOf(JadxRuntimeException.class)
					.hasMessageContaining("Cyclic parent plugins detected");
		}
	}

	private JadxPluginsTools newTools(Path dropinsDir) {
		return new JadxPluginsTools(
				tempDir.resolve("plugins.json"),
				tempDir.resolve("installed"),
				dropinsDir,
				Collections::emptyList);
	}

	private static PluginRuntime findPlugin(SortedSet<PluginRuntime> plugins, String pluginId) {
		return plugins.stream()
				.filter(p -> pluginId.equals(p.getPluginId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Plugin not found: " + pluginId));
	}

	@SuppressWarnings("unchecked")
	private static List<String> readEvents(JadxPlugin parentPlugin) throws Exception {
		return (List<String>) parentPlugin.getClass().getMethod("getEvents").invoke(parentPlugin);
	}

	private Path buildParentPluginJar(Path outDir, String jarName, String pluginId) throws IOException {
		String apiSrc = "package p0parent;\n"
				+ "public class ParentApi {\n"
				+ "    public static String greet(String name) {\n"
				+ "        return \"hello-\" + name;\n"
				+ "    }\n"
				+ "}\n";
		String pluginSrc = "package p0parent;\n"
				+ "import java.util.ArrayList;\n"
				+ "import java.util.List;\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "public class ParentPlugin implements JadxPlugin {\n"
				+ "    private final List<String> events = new ArrayList<>();\n"
				+ "    public void recordEvent(String event) {\n"
				+ "        events.add(event);\n"
				+ "    }\n"
				+ "    public List<String> getEvents() {\n"
				+ "        return events;\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return JadxPluginInfoBuilder.pluginId(\"" + pluginId + "\")\n"
				+ "                .name(\"Parent Plugin\")\n"
				+ "                .description(\"Parent plugin for test\")\n"
				+ "                .build();\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "        events.add(\"parent-init\");\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void unload() {\n"
				+ "        events.add(\"parent-unload\");\n"
				+ "    }\n"
				+ "}\n";
		return compileAndPackageJar(
				outDir.resolve(jarName),
				"p0parent.ParentPlugin",
				new String[] {},
				new SourceFile("p0parent/ParentApi.java", apiSrc),
				new SourceFile("p0parent/ParentPlugin.java", pluginSrc));
	}

	private Path buildChildPluginJar(
			Path outDir, String jarName, String pluginId, String parentId, Path parentJar) throws IOException {
		String helperSrc = "package p1child;\n"
				+ "import p0parent.ParentApi;\n"
				+ "public class ChildApi {\n"
				+ "    public static String childGreet(String name) {\n"
				+ "        return ParentApi.greet(name);\n"
				+ "    }\n"
				+ "}\n";
		String pluginSrc = "package p1child;\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "import p0parent.ParentApi;\n"
				+ "import p0parent.ParentPlugin;\n"
				+ "public class ChildPlugin implements JadxPlugin {\n"
				+ "    private ParentPlugin parentPlugin;\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return JadxPluginInfoBuilder.pluginId(\"" + pluginId + "\")\n"
				+ "                .name(\"Child Plugin\")\n"
				+ "                .description(\"Child plugin for test\")\n"
				+ "                .parentPlugin(\"" + parentId + "\")\n"
				+ "                .build();\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "        this.parentPlugin = context.plugins().getInstance(ParentPlugin.class);\n"
				+ "        this.parentPlugin.recordEvent(\"child-init:\" + ParentApi.greet(\"" + pluginId + "\"));\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void unload() {\n"
				+ "        if (parentPlugin != null) {\n"
				+ "            parentPlugin.recordEvent(\"child-unload\");\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		return compileAndPackageJar(
				outDir.resolve(jarName),
				"p1child.ChildPlugin",
				new String[] { parentJar.toString() },
				new SourceFile("p1child/ChildApi.java", helperSrc),
				new SourceFile("p1child/ChildPlugin.java", pluginSrc));
	}

	private Path buildGrandChildPluginJar(
			Path outDir, String jarName, String pluginId, String parentId, Path parentJar, Path childJar) throws IOException {
		String pluginSrc = "package p2grand;\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "import p0parent.ParentApi;\n"
				+ "import p0parent.ParentPlugin;\n"
				+ "import p1child.ChildApi;\n"
				+ "public class GrandChildPlugin implements JadxPlugin {\n"
				+ "    private ParentPlugin parentPlugin;\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return JadxPluginInfoBuilder.pluginId(\"" + pluginId + "\")\n"
				+ "                .name(\"GrandChild Plugin\")\n"
				+ "                .description(\"GrandChild plugin for test\")\n"
				+ "                .parentPlugin(\"" + parentId + "\")\n"
				+ "                .build();\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "        this.parentPlugin = context.plugins().getInstance(ParentPlugin.class);\n"
				+ "        String msg = \"grandchild-init:\" + ParentApi.greet(\"" + pluginId + "\")\n"
				+ "                + \":\" + ChildApi.childGreet(\"" + parentId + "\");\n"
				+ "        this.parentPlugin.recordEvent(msg);\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void unload() {\n"
				+ "        if (parentPlugin != null) {\n"
				+ "            parentPlugin.recordEvent(\"grandchild-unload\");\n"
				+ "        }\n"
				+ "    }\n"
				+ "}\n";
		return compileAndPackageJar(
				outDir.resolve(jarName),
				"p2grand.GrandChildPlugin",
				new String[] { parentJar.toString(), childJar.toString() },
				new SourceFile("p2grand/GrandChildPlugin.java", pluginSrc));
	}

	private Path buildSimpleParentingJar(
			Path outDir, String jarName, String pkg, String clsName, String pluginId, String parentId) throws IOException {
		String pluginSrc = "package " + pkg + ";\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "public class " + clsName + " implements JadxPlugin {\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return JadxPluginInfoBuilder.pluginId(\"" + pluginId + "\")\n"
				+ "                .name(\"" + clsName + "\")\n"
				+ "                .description(\"Cycle test\")\n"
				+ "                .parentPlugin(\"" + parentId + "\")\n"
				+ "                .build();\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "    }\n"
				+ "}\n";
		return compileAndPackageJar(
				outDir.resolve(jarName),
				pkg + '.' + clsName,
				new String[] {},
				new SourceFile(pkg + '/' + clsName + ".java", pluginSrc));
	}

	private Path compileAndPackageJar(
			Path jarPath,
			String serviceImplClass,
			String[] extraCp,
			SourceFile... sources) throws IOException {
		Path workDir = Files.createTempDirectory(tempDir, "compile");
		Path srcDir = workDir.resolve("src");
		Path classesDir = workDir.resolve("classes");
		Files.createDirectories(classesDir);

		List<String> srcPaths = new ArrayList<>();
		for (SourceFile source : sources) {
			Path file = srcDir.resolve(source.relativePath);
			Files.createDirectories(file.getParent());
			Files.writeString(file, source.content, StandardCharsets.UTF_8);
			srcPaths.add(file.toString());
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
			List<Path> classFiles = walk.filter(Files::isRegularFile).collect(Collectors.toList());
			for (Path classFile : classFiles) {
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

	private static final class SourceFile {
		final String relativePath;
		final String content;

		SourceFile(String relativePath, String content) {
			this.relativePath = relativePath;
			this.content = content;
		}
	}
}
