package jadx.cli;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;

import jadx.api.plugins.gui.IJadxGuiPlugin;
import jadx.core.plugins.files.SingleDirFilesGetter;
import jadx.plugins.tools.JadxExternalPluginsLoader;
import jadx.plugins.tools.JadxPluginsTools;

import static org.assertj.core.api.Assertions.assertThat;

public class CliPluginDependenciesTest extends BaseCliIntegrationTest {

	@Test
	void cliLoadsParentBeforeChildAndDisablePluginsPrunesChild() throws Exception {
		Path dropinsDir = testDir.resolve("dropins");
		Files.createDirectories(dropinsDir);

		Path parentJar = compilePluginJar(
				dropinsDir.resolve("z-parent.jar"),
				"cliparent.CliParentPlugin",
				Map.of(
						"cliparent/MarkerApi.java",
						"package cliparent;\n"
								+ "import java.io.IOException;\n"
								+ "import java.nio.charset.StandardCharsets;\n"
								+ "import java.nio.file.Files;\n"
								+ "import java.nio.file.Path;\n"
								+ "import java.nio.file.Paths;\n"
								+ "import java.nio.file.StandardOpenOption;\n"
								+ "public class MarkerApi {\n"
								+ "    private static String markerPath;\n"
								+ "    public static void setMarkerPath(String path) { markerPath = path; }\n"
								+ "    public static void append(String line) {\n"
								+ "        if (markerPath == null || markerPath.isEmpty()) return;\n"
								+ "        try {\n"
								+ "            Path p = Paths.get(markerPath);\n"
								+ "            Files.writeString(p, line + \"\\n\", StandardCharsets.UTF_8,\n"
								+ "                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);\n"
								+ "        } catch (IOException e) { throw new RuntimeException(e); }\n"
								+ "    }\n"
								+ "}\n",
						"cliparent/CliParentPlugin.java",
						"package cliparent;\n"
								+ "import jadx.api.plugins.*;\n"
								+ "import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;\n"
								+ "public class CliParentPlugin implements JadxPlugin {\n"
								+ "    private final Opts opts = new Opts();\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"z-parent\")\n"
								+ "                .name(\"CLI Parent\").description(\"test\").build();\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {\n"
								+ "        context.registerOptions(opts);\n"
								+ "        MarkerApi.setMarkerPath(context.getArgs().getPluginOptions().get(\"z-parent.marker\"));\n"
								+ "        MarkerApi.append(\"parent\");\n"
								+ "    }\n"
								+ "    private static class Opts extends BasePluginOptionsBuilder {\n"
								+ "        @Override public void registerOptions() {\n"
								+ "            strOption(\"z-parent.marker\").description(\"m\").defaultValue(\"\").setter(v -> {});\n"
								+ "        }\n"
								+ "    }\n"
								+ "}\n"),
				new String[] {});

		compilePluginJar(
				dropinsDir.resolve("a-child.jar"),
				"clichild.CliChildPlugin",
				Map.of(
						"clichild/CliChildPlugin.java",
						"package clichild;\n"
								+ "import java.io.IOException;\n"
								+ "import java.nio.charset.StandardCharsets;\n"
								+ "import java.nio.file.Files;\n"
								+ "import java.nio.file.Paths;\n"
								+ "import jadx.api.plugins.*;\n"
								+ "import jadx.api.plugins.options.impl.BasePluginOptionsBuilder;\n"
								+ "import cliparent.MarkerApi;\n"
								+ "public class CliChildPlugin implements JadxPlugin {\n"
								+ "    private final Opts opts = new Opts();\n"
								+ "    @Override public JadxPluginInfo getPluginInfo() {\n"
								+ "        return JadxPluginInfoBuilder.pluginId(\"a-child\")\n"
								+ "                .name(\"CLI Child\").description(\"test\")\n"
								+ "                .parentPlugin(\"z-parent\").build();\n"
								+ "    }\n"
								+ "    @Override public void init(JadxPluginContext context) {\n"
								+ "        context.registerOptions(opts);\n"
								+ "        String childMarker = context.getArgs().getPluginOptions().get(\"a-child.marker\");\n"
								+ "        if (childMarker != null && !childMarker.isEmpty()) {\n"
								+ "            try {\n"
								+ "                Files.writeString(Paths.get(childMarker), \"child-ran\", StandardCharsets.UTF_8);\n"
								+ "            } catch (IOException e) { throw new RuntimeException(e); }\n"
								+ "        }\n"
								+ "        MarkerApi.append(\"child\");\n"
								+ "    }\n"
								+ "    private static class Opts extends BasePluginOptionsBuilder {\n"
								+ "        @Override public void registerOptions() {\n"
								+ "            strOption(\"a-child.marker\").description(\"m\").defaultValue(\"\").setter(v -> {});\n"
								+ "        }\n"
								+ "    }\n"
								+ "}\n"),
				new String[] { parentJar.toString() });

		JadxPluginsTools tools = new JadxPluginsTools(
				testDir.resolve("plugins.json"),
				testDir.resolve("installed"),
				dropinsDir,
				Collections::emptyList);

		Path marker1 = testDir.resolve("marker-enabled.txt");
		Path childMarker1 = testDir.resolve("child-marker-enabled.txt");
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(
				cls -> !IJadxGuiPlugin.class.isAssignableFrom(cls), tools)) {
			String[] args = buildArgs(
					List.of("-Pz-parent.marker=" + marker1.toAbsolutePath(),
							"-Pa-child.marker=" + childMarker1.toAbsolutePath()),
					"samples/hello.dex");
			int code = JadxCLI.execute(args, jadxArgs -> {
				jadxArgs.setFilesGetter(new SingleDirFilesGetter(testDir));
				jadxArgs.setPluginLoader(loader);
			});
			assertThat(code).isZero();
		}
		assertThat(Files.readAllLines(marker1)).containsExactly("parent", "child");
		assertThat(Files.readString(childMarker1)).isEqualTo("child-ran");

		Path marker2 = testDir.resolve("marker-disabled.txt");
		Path childMarker2 = testDir.resolve("child-marker-disabled.txt");
		try (JadxExternalPluginsLoader loader = new JadxExternalPluginsLoader(
				cls -> !IJadxGuiPlugin.class.isAssignableFrom(cls), tools)) {
			String[] args = buildArgs(
					List.of("--disable-plugins", "z-parent",
							"-Pz-parent.marker=" + marker2.toAbsolutePath(),
							"-Pa-child.marker=" + childMarker2.toAbsolutePath()),
					"samples/hello.dex");
			int code = JadxCLI.execute(args, jadxArgs -> {
				jadxArgs.setFilesGetter(new SingleDirFilesGetter(testDir));
				jadxArgs.setPluginLoader(loader);
			});
			assertThat(code).isZero();
		}
		assertThat(Files.exists(marker2)).isFalse();
		assertThat(Files.exists(childMarker2)).isFalse();
	}

	private Path compilePluginJar(
			Path jarPath,
			String serviceImplClass,
			Map<String, String> sourceFiles,
			String[] extraCp) throws IOException {
		Path workDir = Files.createTempDirectory(testDir, "compile");
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
}
