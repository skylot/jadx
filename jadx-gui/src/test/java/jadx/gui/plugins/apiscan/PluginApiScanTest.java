package jadx.gui.plugins.apiscan;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;

import static jadx.gui.plugins.apiscan.ApiCategory.EXIT;
import static jadx.gui.plugins.apiscan.ApiCategory.FILES;
import static jadx.gui.plugins.apiscan.ApiCategory.PROCESS;
import static jadx.gui.plugins.apiscan.ApiCategory.REFLECTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class PluginApiScanTest {
	private static final String EXEC_COMMENT = "Dangerous API: java.lang.Runtime.exec (Process execution)";

	private static final List<String> PLUGIN_SRC = Arrays.asList(
			"package evil;",
			"import java.lang.reflect.Field;",
			"import java.lang.reflect.Method;",
			"import java.nio.file.Files;",
			"import java.nio.file.Paths;",
			"import java.util.function.IntConsumer;",
			"public class EvilPlugin {",
			"	public void run() throws Exception {",
			"		Runtime.getRuntime().exec(\"calc\");",
			"		new ProcessBuilder(\"calc\").start();",
			"		Field field = String.class.getDeclaredField(\"value\");",
			"		field.setAccessible(true);",
			"		Method method = Object.class.getDeclaredMethod(\"clone\");",
			"		method.setAccessible(true);",
			"		Files.readAllBytes(Paths.get(\"secret\"));",
			"	}",
			"	public IntConsumer exitLater() {",
			"		return System::exit;",
			"	}",
			"	public int safe(int a) {",
			"		return a * 2;",
			"	}",
			"}");

	private static final List<String> REACH_PLUGIN_SRC = Arrays.asList(
			"package evil;",
			"public class EvilPlugin {",
			"	public void init() {",
			"		Helper.run();",
			"	}",
			"}",
			"class Helper {",
			"	static void run() {",
			"		new Thread(new Runnable() {",
			"			public void run() {",
			"				System.exit(0);",
			"			}",
			"		}).start();",
			"		try {",
			"			Runtime.getRuntime().exec(\"calc\");",
			"		} catch (Exception e) {",
			"			throw new RuntimeException(e);",
			"		}",
			"	}",
			"}",
			"class Other {",
			"	void delete() {",
			"		new java.io.File(\"data\").delete();",
			"	}",
			"}");

	@TempDir
	Path tempDir;

	@Test
	void scanPlugin() throws Exception {
		try (JadxDecompiler jadx = new JadxDecompiler(buildArgs(PLUGIN_SRC, true))) {
			PluginApiScan.register(jadx);
			jadx.load();

			ApiScanResult result = ApiScanNode.getScanResult(jadx);
			assertThat(result).isNotNull();
			assertThat(result.getUsages())
					.extracting(u -> u.getMth().getName(), ApiUsage::getCategory, ApiUsage::getApi)
					.containsExactlyInAnyOrder(
							tuple("run", PROCESS, "java.lang.Runtime.exec"),
							tuple("run", PROCESS, "java.lang.ProcessBuilder.start"),
							tuple("run", REFLECTION, "java.lang.reflect.Field.setAccessible"),
							tuple("run", REFLECTION, "java.lang.reflect.Method.setAccessible"),
							tuple("run", FILES, "java.nio.file.Files.readAllBytes"),
							tuple("exitLater", EXIT, "java.lang.System.exit"));

			JavaClass cls = jadx.searchJavaClassByOrigFullName("evil.EvilPlugin");
			assertThat(cls.getCode()).containsOnlyOnce(EXEC_COMMENT);
			cls.reload();
			assertThat(cls.getCode()).containsOnlyOnce(EXEC_COMMENT);
		}
	}

	@Test
	void reachableFromEntry() throws Exception {
		try (JadxDecompiler jadx = new JadxDecompiler(buildArgs(REACH_PLUGIN_SRC, true))) {
			PluginApiScan.register(jadx);
			jadx.load();

			ApiScanResult result = ApiScanNode.getScanResult(jadx);
			assertThat(result).isNotNull();
			assertThat(result.getUsages())
					.extracting(ApiUsage::getApi, result::isReachable)
					.containsExactlyInAnyOrder(
							tuple("java.lang.Runtime.exec", true),
							tuple("java.lang.System.exit", true),
							tuple("java.io.File.delete", false));
		}
	}

	@Test
	void skipNotPlugin() throws Exception {
		try (JadxDecompiler jadx = new JadxDecompiler(buildArgs(PLUGIN_SRC, false))) {
			PluginApiScan.register(jadx);
			jadx.load();

			ApiScanResult result = ApiScanNode.getScanResult(jadx);
			assertThat(result).isNull();
			JavaClass cls = jadx.searchJavaClassByOrigFullName("evil.EvilPlugin");
			assertThat(cls.getCode()).doesNotContain("Dangerous API");
		}
	}

	private JadxArgs buildArgs(List<String> src, boolean addServiceFile) throws Exception {
		Path srcFile = tempDir.resolve("src/evil/EvilPlugin.java");
		Files.createDirectories(srcFile.getParent());
		Files.write(srcFile, src);
		Path classesDir = tempDir.resolve("classes");
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		int compileResult = compiler.run(null, null, null, "-d", classesDir.toString(), srcFile.toString());
		assertThat(compileResult).isZero();

		Path jar = tempDir.resolve("evil-plugin.jar");
		try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar));
				Stream<Path> files = Files.walk(classesDir)) {
			for (Path clsFile : files.filter(f -> f.toString().endsWith(".class")).collect(Collectors.toList())) {
				out.putNextEntry(new JarEntry(classesDir.relativize(clsFile).toString().replace('\\', '/')));
				out.write(Files.readAllBytes(clsFile));
				out.closeEntry();
			}
			if (addServiceFile) {
				out.putNextEntry(new JarEntry("META-INF/services/jadx.api.plugins.JadxPlugin"));
				out.write("evil.EvilPlugin".getBytes(StandardCharsets.UTF_8));
				out.closeEntry();
			}
		}
		JadxArgs args = new JadxArgs();
		args.setInputFile(jar.toFile());
		return args;
	}
}
