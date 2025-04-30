package jadx.cli;

import java.nio.file.Path;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInput extends BaseCliIntegrationTest {

	@Test
	public void testHelp() {
		int result = execJadxCli(new String[] { "--help" });
		assertThat(result).isEqualTo(0);
	}

	@Test
	public void testApkInput() throws Exception {
		int result = execJadxCli(buildArgs(List.of(), "samples/small.apk"));
		assertThat(result).isEqualTo(0);
		assertThat(collectAllFilesInDir(outputDir))
				.describedAs("check output files")
				.map(p -> p.getFileName().toString())
				.haveExactly(2, new Condition<>(f -> f.endsWith(".java"), "java classes"))
				.haveExactly(9, new Condition<>(f -> f.endsWith(".xml"), "xml resources"))
				.haveExactly(1, new Condition<>(f -> f.equals("AndroidManifest.xml"), "manifest"))
				.hasSize(12);
	}

	@Test
	public void testDexInput() throws Exception {
		decompile("samples/hello.dex");
	}

	@Test
	public void testSmaliInput() throws Exception {
		decompile("samples/HelloWorld.smali");
	}

	@Test
	public void testClassInput() throws Exception {
		decompile("samples/HelloWorld.class");
	}

	@Test
	public void testMultipleInput() throws Exception {
		decompile("samples/hello.dex", "samples/HelloWorld.smali");
	}

	@Test
	public void testFallbackMode() throws Exception {
		int result = execJadxCli(buildArgs(List.of("-f"), "samples/hello.dex"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(outputDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testSimpleMode() throws Exception {
		int result = execJadxCli(buildArgs(List.of("--decompilation-mode", "simple"), "samples/hello.dex"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(outputDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testResourceOnly() throws Exception {
		int result = execJadxCli(buildArgs(List.of(), "samples/resources-only.apk"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectFilesInDir(outputDir,
				path -> path.getFileName().toString().equalsIgnoreCase("AndroidManifest.xml"));
		assertThat(files).isNotEmpty();
	}
}
