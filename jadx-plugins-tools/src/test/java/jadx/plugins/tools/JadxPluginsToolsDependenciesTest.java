package jadx.plugins.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.plugins.JadxPlugin;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.plugins.tools.data.JadxPluginListEntry;
import jadx.plugins.tools.data.JadxPluginMetadata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JadxPluginsToolsDependenciesTest {

	@TempDir
	Path tempDir;

	@Test
	void installAutoInstallsDependencyAndParentPlugins() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path depJar = buildPluginJar(jarsDir, "dep.jar", "dep", "DepPlugin", "dep-plugin", null, Map.of());
		Path parentJar = buildPluginJar(jarsDir, "parent.jar", "parent", "ParentPlugin", "parent-plugin", null, Map.of());
		Map<String, String> targetDeps = new LinkedHashMap<>();
		targetDeps.put("dep-plugin", "");
		Path targetJar = buildPluginJar(
				jarsDir, "target.jar", "target", "TargetPlugin", "target-plugin", "parent-plugin", targetDeps);

		List<JadxPluginListEntry> registry = List.of(
				listEntry("dep-plugin", "file:" + depJar),
				listEntry("parent-plugin", "file:" + parentJar));
		JadxPluginsTools tools = newTools(registry, tempDir.resolve("dropins"));

		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + targetJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("dep-plugin", "parent-plugin", "target-plugin");

		JadxPluginMetadata storedTarget = tools.getInstalled().stream()
				.filter(m -> "target-plugin".equals(m.getPluginId()))
				.findFirst()
				.orElseThrow();
		assertThat(storedTarget.getParentPlugin()).isEqualTo("parent-plugin");
		assertThat(storedTarget.getDependencyPlugins()).containsExactly("dep-plugin");

		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools)) {
			List<JadxPlugin> loaded = loader.load();
			ClassLoader parentCl = findPlugin(loaded, "parent-plugin").getClass().getClassLoader();
			ClassLoader targetCl = findPlugin(loaded, "target-plugin").getClass().getClassLoader();
			assertThat(targetCl.getParent()).isSameAs(parentCl);
		}
	}

	@Test
	void installSupportsExplicitDependencyLocationsAndTransitiveChain() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path leafJar = buildPluginJar(jarsDir, "leaf.jar", "leaf", "LeafPlugin", "leaf-plugin", null, Map.of());
		Path midJar = buildPluginJar(
				jarsDir, "mid.jar", "mid", "MidPlugin", "mid-plugin", null,
				Map.of("leaf-plugin", "file:" + leafJar));
		Path topJar = buildPluginJar(
				jarsDir, "top.jar", "top", "TopPlugin", "top-plugin", null,
				Map.of("mid-plugin", ""));

		// Only mid-plugin is in registry; leaf-plugin is resolved via explicit location in mid-plugin.
		JadxPluginsTools tools = newTools(
				List.of(listEntry("mid-plugin", "file:" + midJar)),
				tempDir.resolve("dropins"));

		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + topJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("leaf-plugin", "mid-plugin", "top-plugin");

		JadxPluginMetadata storedMid = tools.getInstalled().stream()
				.filter(m -> "mid-plugin".equals(m.getPluginId()))
				.findFirst()
				.orElseThrow();
		assertThat(storedMid.getDependencyLocations()).containsEntry("leaf-plugin", "file:" + leafJar);
	}

	@Test
	void installSupportsDiamondDependenciesWithoutDuplication() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path sharedJar = buildPluginJar(jarsDir, "shared.jar", "shared", "SharedPlugin", "shared-plugin", null, Map.of());
		Path leftJar = buildPluginJar(
				jarsDir, "left.jar", "left", "LeftPlugin", "left-plugin", null,
				Map.of("shared-plugin", ""));
		Path rightJar = buildPluginJar(
				jarsDir, "right.jar", "right", "RightPlugin", "right-plugin", null,
				Map.of("shared-plugin", ""));
		Map<String, String> appDeps = new LinkedHashMap<>();
		appDeps.put("left-plugin", "");
		appDeps.put("right-plugin", "");
		Path appJar = buildPluginJar(jarsDir, "app.jar", "app", "AppPlugin", "app-plugin", null, appDeps);

		List<JadxPluginListEntry> registry = List.of(
				listEntry("shared-plugin", "file:" + sharedJar),
				listEntry("left-plugin", "file:" + leftJar),
				listEntry("right-plugin", "file:" + rightJar));
		JadxPluginsTools tools = newTools(registry, tempDir.resolve("dropins"));

		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + appJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("shared-plugin", "left-plugin", "right-plugin", "app-plugin");
		assertThat(tools.getInstalled())
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactlyInAnyOrder("shared-plugin", "left-plugin", "right-plugin", "app-plugin");
	}

	@Test
	void installReenablesDisabledDependency() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path depJar = buildPluginJar(jarsDir, "dep.jar", "dep", "DepPlugin", "dep-plugin", null, Map.of());
		Path targetJar = buildPluginJar(
				jarsDir, "target.jar", "target", "TargetPlugin", "target-plugin", null,
				Map.of("dep-plugin", ""));

		JadxPluginsTools tools = newTools(List.of(), tempDir.resolve("dropins"));
		tools.install("file:" + depJar);
		assertThat(tools.changeDisabledStatus("dep-plugin", true)).isTrue();
		assertThat(tools.getEnabledInstalled()).isEmpty();

		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + targetJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("dep-plugin", "target-plugin");
		assertThat(tools.getEnabledInstalled())
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactlyInAnyOrder("dep-plugin", "target-plugin");
	}

	@Test
	void installSatisfiedByDropinDoesNotDuplicateInstall() throws Exception {
		Path dropinsDir = tempDir.resolve("dropins");
		Files.createDirectories(dropinsDir);
		Path jarsDir = tempDir.resolve("jars");

		buildPluginJar(dropinsDir, "dropin-dep.jar", "dropdep", "DropDepPlugin", "dropin-dep", null, Map.of());
		Path targetJar = buildPluginJar(
				jarsDir, "target.jar", "target", "TargetPlugin", "target-plugin", null,
				Map.of("dropin-dep", ""));

		JadxPluginsTools tools = newTools(List.of(), dropinsDir);
		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + targetJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("target-plugin");
		assertThat(tools.getInstalled())
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("target-plugin");
	}

	@Test
	void installReenablesTransitiveDisabledDependencies() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path leafJar = buildPluginJar(jarsDir, "leaf.jar", "leaf", "LeafPlugin", "leaf-plugin", null, Map.of());
		Path midJar = buildPluginJar(
				jarsDir, "mid.jar", "mid", "MidPlugin", "mid-plugin", "leaf-plugin", Map.of());
		Path topJar = buildPluginJar(
				jarsDir, "top.jar", "top", "TopPlugin", "top-plugin", null,
				Map.of("mid-plugin", ""));

		JadxPluginsTools tools = newTools(List.of(), tempDir.resolve("dropins"));
		tools.install("file:" + leafJar);
		tools.install("file:" + midJar);
		assertThat(tools.changeDisabledStatus("mid-plugin", true)).isTrue();
		assertThat(tools.changeDisabledStatus("leaf-plugin", true)).isTrue();
		assertThat(tools.getEnabledInstalled()).isEmpty();

		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + topJar);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("leaf-plugin", "mid-plugin", "top-plugin");
		assertThat(tools.getEnabledInstalled())
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactlyInAnyOrder("leaf-plugin", "mid-plugin", "top-plugin");
	}

	@Test
	void installZipDirectoryPluginWithParentAndBundledLib() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path parentPluginJar = buildCustomJar(
				jarsDir.resolve("z-parent-plugin.jar"),
				"pzip.ParentPlugin",
				Map.of(
						"pzip/ParentHelper.java",
						"package pzip;\npublic class ParentHelper { public static String tag() { return \"P\"; } }\n",
						"pzip/ParentPlugin.java",
						"package pzip;\n"
								+ "import jadx.api.plugins.*;\n"
								+ "public class ParentPlugin implements JadxPlugin {\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"zip-parent\").name(\"P\").description(\"d\").build();\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {}\n"
								+ "}\n"),
				new String[] {});
		Path helperLibJar = buildCustomJar(
				jarsDir.resolve("helper-lib.jar"),
				null,
				Map.of(
						"plib/ExtraLib.java",
						"package plib;\npublic class ExtraLib { public static String tag() { return \"L\"; } }\n"),
				new String[] {});
		Path childJar = buildCustomJar(
				jarsDir.resolve("a-child.jar"),
				"czip.ChildZipPlugin",
				Map.of(
						"czip/ChildZipPlugin.java",
						"package czip;\n"
								+ "import jadx.api.plugins.*;\n"
								+ "import pzip.ParentHelper;\n"
								+ "import plib.ExtraLib;\n"
								+ "public class ChildZipPlugin implements JadxPlugin {\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"zip-child\").name(\"C\").description(\"d\")\n"
								+ "                .parentPlugin(\"zip-parent\").build();\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {}\n"
								+ "    public String combined() { return ParentHelper.tag() + ExtraLib.tag(); }\n"
								+ "}\n"),
				new String[] { parentPluginJar.toString(), helperLibJar.toString() });

		// Package child + helper-lib into a directory inside a .zip plugin archive
		Path childZip = jarsDir.resolve("zip-child.zip");
		try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(Files.newOutputStream(childZip))) {
			zos.putNextEntry(new java.util.zip.ZipEntry("a-child.jar"));
			zos.write(Files.readAllBytes(childJar));
			zos.closeEntry();
			zos.putNextEntry(new java.util.zip.ZipEntry("helper-lib.jar"));
			zos.write(Files.readAllBytes(helperLibJar));
			zos.closeEntry();
		}

		JadxPluginsTools tools = newTools(
				List.of(listEntry("zip-parent", "file:" + parentPluginJar)),
				tempDir.resolve("dropins"));
		List<JadxPluginMetadata> installed = tools.installPlugins("file:" + childZip);
		assertThat(installed)
				.extracting(JadxPluginMetadata::getPluginId)
				.containsExactly("zip-parent", "zip-child");

		JadxPluginMetadata childMeta = installed.get(1);
		assertThat(Files.isDirectory(tools.getInstalledPluginPath(childMeta))).isTrue();

		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(c -> true, tools)) {
			List<JadxPlugin> loaded = loader.load();
			JadxPlugin loadedParent = findPlugin(loaded, "zip-parent");
			JadxPlugin loadedChild = findPlugin(loaded, "zip-child");
			assertThat(loadedChild.getClass().getClassLoader().getParent())
					.isSameAs(loadedParent.getClass().getClassLoader());
			assertThat(loadedChild.getClass().getMethod("combined").invoke(loadedChild)).isEqualTo("PL");
		}
	}

	@Test
	void installRejectsCircularDependencies() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path cycAJar = buildPluginJar(
				jarsDir, "cyc-a.jar", "cyca", "CycAPlugin", "cyc-a", null, Map.of("cyc-b", ""));
		Path cycBJar = buildPluginJar(
				jarsDir, "cyc-b.jar", "cycb", "CycBPlugin", "cyc-b", null, Map.of("cyc-a", ""));

		List<JadxPluginListEntry> registry = List.of(
				listEntry("cyc-a", "file:" + cycAJar),
				listEntry("cyc-b", "file:" + cycBJar));
		JadxPluginsTools tools = newTools(registry, tempDir.resolve("dropins"));

		assertThatThrownBy(() -> tools.installPlugins("file:" + cycAJar))
				.isInstanceOf(JadxRuntimeException.class)
				.hasMessageContaining("Circular plugin dependency detected");
		assertThat(tools.getInstalled()).isEmpty();
	}

	@Test
	void installFailsWhenRequiredDependencyNotFoundInRegistry() throws Exception {
		Path jarsDir = tempDir.resolve("jars");
		Path targetJar = buildPluginJar(
				jarsDir, "target.jar", "target", "TargetPlugin", "target-plugin", null,
				Map.of("missing-plugin", ""));

		JadxPluginsTools tools = newTools(List.of(), tempDir.resolve("dropins"));
		assertThatThrownBy(() -> tools.installPlugins("file:" + targetJar))
				.isInstanceOf(JadxRuntimeException.class)
				.hasMessageContaining("missing-plugin");
		assertThat(tools.getInstalled()).isEmpty();
	}

	private JadxPluginsTools newTools(List<JadxPluginListEntry> entries, Path dropinsDir) {
		return new JadxPluginsTools(
				tempDir.resolve("plugins.json"),
				tempDir.resolve("installed"),
				dropinsDir,
				() -> entries);
	}

	private static JadxPluginListEntry listEntry(String pluginId, String locationId) {
		JadxPluginListEntry entry = new JadxPluginListEntry();
		entry.setPluginId(pluginId);
		entry.setLocationId(locationId);
		return entry;
	}

	private static JadxPlugin findPlugin(List<JadxPlugin> plugins, String pluginId) {
		return plugins.stream()
				.filter(p -> p.getPluginInfo() != null && pluginId.equals(p.getPluginInfo().getPluginId()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("Plugin not loaded: " + pluginId));
	}

	private Path buildPluginJar(
			Path outDir,
			String jarName,
			String pkg,
			String clsName,
			String pluginId,
			String parentPlugin,
			Map<String, String> deps) throws IOException {
		StringBuilder builderCalls = new StringBuilder();
		builderCalls.append("JadxPluginInfoBuilder.pluginId(\"").append(pluginId).append("\")\n")
				.append("                .name(\"").append(clsName).append("\")\n")
				.append("                .description(\"Test plugin\")\n");
		if (parentPlugin != null) {
			builderCalls.append("                .parentPlugin(\"").append(parentPlugin).append("\")\n");
		}
		for (Map.Entry<String, String> dep : deps.entrySet()) {
			if (dep.getValue() == null || dep.getValue().isEmpty()) {
				builderCalls.append("                .dependencyPlugin(\"").append(dep.getKey()).append("\")\n");
			} else {
				builderCalls.append("                .dependencyPlugin(\"").append(dep.getKey())
						.append("\", \"").append(escapeJava(dep.getValue())).append("\")\n");
			}
		}
		builderCalls.append("                .build()");

		String src = "package " + pkg + ";\n"
				+ "import jadx.api.plugins.JadxPlugin;\n"
				+ "import jadx.api.plugins.JadxPluginContext;\n"
				+ "import jadx.api.plugins.JadxPluginInfo;\n"
				+ "import jadx.api.plugins.JadxPluginInfoBuilder;\n"
				+ "public class " + clsName + " implements JadxPlugin {\n"
				+ "    @Override\n"
				+ "    public JadxPluginInfo getPluginInfo() {\n"
				+ "        return " + builderCalls + ";\n"
				+ "    }\n"
				+ "    @Override\n"
				+ "    public void init(JadxPluginContext context) {\n"
				+ "    }\n"
				+ "}\n";

		Path workDir = Files.createTempDirectory(tempDir, "compile");
		Path srcFile = workDir.resolve("src/" + pkg + '/' + clsName + ".java");
		Path classesDir = workDir.resolve("classes");
		Files.createDirectories(srcFile.getParent());
		Files.createDirectories(classesDir);
		Files.writeString(srcFile, src, StandardCharsets.UTF_8);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int result = compiler.run(null, null, null,
				"-cp", System.getProperty("java.class.path"),
				"-d", classesDir.toString(),
				srcFile.toString());
		assertThat(result).isZero();

		Path jarPath = outDir.resolve(jarName);
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
			out.write((pkg + '.' + clsName + "\n").getBytes(StandardCharsets.UTF_8));
			out.closeEntry();
		}
		return jarPath;
	}

	private Path buildCustomJar(
			Path jarPath,
			String serviceImplClass,
			Map<String, String> sourceFiles,
			String[] extraCp) throws IOException {
		Path workDir = Files.createTempDirectory(tempDir, "customcompile");
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
			cp.append(java.io.File.pathSeparator).append(entry);
		}

		List<String> args = new ArrayList<>();
		args.add("-cp");
		args.add(cp.toString());
		args.add("-d");
		args.add(classesDir.toString());
		args.addAll(srcPaths);

		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int result = compiler.run(null, null, null, args.toArray(new String[0]));
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
			if (serviceImplClass != null) {
				out.putNextEntry(new JarEntry("META-INF/services/jadx.api.plugins.JadxPlugin"));
				out.write((serviceImplClass + "\n").getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		return jarPath;
	}

	private static String escapeJava(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
