package jadx.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInput {
	private static final Logger LOG = LoggerFactory.getLogger(TestInput.class);

	private static final PathMatcher LOG_ALL_FILES = path -> {
		LOG.debug("File in result dir: {}", path);
		return true;
	};

	@TempDir
	Path testDir;

	@Test
	public void testHelp() {
		int result = JadxCLI.execute(new String[] { "--help" });
		assertThat(result).isEqualTo(0);
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
		int result = JadxCLI.execute(buildArgs(List.of("-f"), "samples/hello.dex"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(testDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testSimpleMode() throws Exception {
		int result = JadxCLI.execute(buildArgs(List.of("--decompilation-mode", "simple"), "samples/hello.dex"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(testDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testResourceOnly() throws Exception {
		int result = JadxCLI.execute(buildArgs(List.of(), "samples/resources-only.apk"));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectFilesInDir(testDir,
				path -> path.getFileName().toString().equalsIgnoreCase("AndroidManifest.xml"));
		assertThat(files).isNotEmpty();
	}

	private void decompile(String... inputSamples) throws URISyntaxException, IOException {
		int result = JadxCLI.execute(buildArgs(List.of(), inputSamples));
		assertThat(result).isEqualTo(0);
		List<Path> resultJavaFiles = collectJavaFilesInDir(testDir);
		assertThat(resultJavaFiles).isNotEmpty();

		// do not copy input files as resources
		for (Path path : collectFilesInDir(testDir, LOG_ALL_FILES)) {
			for (String inputSample : inputSamples) {
				assertThat(path.toAbsolutePath().toString()).doesNotContain(inputSample);
			}
		}
	}

	private String[] buildArgs(List<String> options, String... inputSamples) throws URISyntaxException {
		List<String> args = new ArrayList<>(options);
		args.add("-v");
		args.add("-d");
		args.add(testDir.toAbsolutePath().toString());

		for (String inputSample : inputSamples) {
			URL resource = getClass().getClassLoader().getResource(inputSample);
			assertThat(resource).isNotNull();
			String sampleFile = resource.toURI().getRawPath();
			args.add(sampleFile);
		}
		return args.toArray(new String[0]);
	}

	private static List<Path> collectJavaFilesInDir(Path dir) throws IOException {
		PathMatcher javaMatcher = dir.getFileSystem().getPathMatcher("glob:**.java");
		return collectFilesInDir(dir, javaMatcher);
	}

	private static List<Path> collectFilesInDir(Path dir, PathMatcher matcher) throws IOException {
		try (Stream<Path> pathStream = Files.walk(dir)) {
			return pathStream
					.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
					.filter(matcher::matches)
					.collect(Collectors.toList());
		}
	}
}
