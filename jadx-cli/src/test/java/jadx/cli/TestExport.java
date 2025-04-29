package jadx.cli;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestExport extends BaseCliIntegrationTest {

	@Test
	public void testBasicExport() throws Exception {
		int result = execJadxCli("samples/small.apk");
		assertThat(result).isEqualTo(0);
		assertThat(collectAllFilesInDir(outputDir))
				.map(this::pathToUniformString)
				.haveExactly(2, new Condition<>(f -> f.startsWith("sources/") && f.endsWith(".java"), "sources"))
				.haveExactly(10, new Condition<>(f -> f.startsWith("resources/"), "resources"))
				.haveExactly(1, new Condition<>(f -> f.equals("resources/AndroidManifest.xml"), "manifest"))
				.hasSize(12);
	}

	@Test
	public void testGradleExportApk() throws Exception {
		int result = execJadxCli("samples/small.apk", "--export-gradle");
		assertThat(result).isEqualTo(0);
		assertThat(collectAllFilesInDir(outputDir))
				.describedAs("check output files")
				.map(this::pathToUniformString)
				.haveExactly(2, new Condition<>(f -> f.endsWith(".java"), "java classes"))
				.haveExactly(0, new Condition<>(f -> f.endsWith("classes.dex"), "dex files"))
				.hasSize(15);
	}

	@Test
	public void testGradleExportAAR() throws Exception {
		int result = execJadxCli("samples/test-lib.aar", "--export-gradle");
		assertThat(result).isEqualTo(0);
		assertThat(collectAllFilesInDir(outputDir))
				.describedAs("check output files")
				.map(this::printFileContent)
				.map(this::pathToUniformString)
				.haveExactly(1, new Condition<>(f -> f.startsWith("lib/src/main/java/") && f.endsWith(".java"), "java"))
				.haveExactly(0, new Condition<>(f -> f.endsWith(".jar"), "jar files"))
				.hasSize(8);
	}

	@Test
	public void testGradleExportSimpleJava() throws Exception {
		int result = execJadxCli("samples/HelloWorld.class", "--export-gradle");
		assertThat(result).isEqualTo(0);
		assertThat(collectAllFilesInDir(outputDir))
				.describedAs("check output files")
				.map(this::printFileContent)
				.map(this::pathToUniformString)
				.haveExactly(1, new Condition<>(f -> f.endsWith(".java") && f.startsWith("app/src/main/java/"), "java"))
				.haveExactly(0, new Condition<>(f -> f.endsWith(".class"), "class files"))
				.haveExactly(1, new Condition<>(f -> f.equals("settings.gradle.kts"), "settings"))
				.haveExactly(1, new Condition<>(f -> f.equals("app/build.gradle.kts"), "build"))
				.hasSize(3);
	}

	@Test
	public void testGradleExportInvalidType() throws Exception {
		int result = execJadxCli("samples/HelloWorld.class", "--export-gradle-type", "android-app");
		assertThat(result).isEqualTo(0);
		// expect output in 'android-app' template, but most fields will be set to UNKNOWN.
		assertThat(collectAllFilesInDir(outputDir))
				.describedAs("check output files")
				.map(this::printFileContent)
				.map(this::pathToUniformString)
				.haveExactly(1, new Condition<>(f -> f.endsWith(".java") && f.startsWith("app/src/main/java/"), "java"))
				.haveExactly(1, new Condition<>(f -> f.equals("settings.gradle"), "settings"))
				.haveExactly(1, new Condition<>(f -> f.equals("build.gradle"), "build"))
				.haveExactly(1, new Condition<>(f -> f.equals("app/build.gradle"), "app build"))
				.hasSize(4);
	}
}
