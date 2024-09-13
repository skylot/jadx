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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.utils.files.FileUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInput {
	private static final Logger LOG = LoggerFactory.getLogger(TestInput.class);

	private static final PathMatcher LOG_ALL_FILES = path -> {
		LOG.debug("File in result dir: {}", path);
		return true;
	};

	@Test
	public void testHelp() {
		int result = JadxCLI.execute(new String[] { "--help" });
		assertThat(result).isEqualTo(0);
	}

	@Test
	public void testDexInput() throws Exception {
		decompile("dex", "samples/hello.dex");
	}

	@Test
	public void testSmaliInput() throws Exception {
		decompile("smali", "samples/HelloWorld.smali");
	}

	@Test
	public void testClassInput() throws Exception {
		decompile("class", "samples/HelloWorld.class");
	}

	@Test
	public void testMultipleInput() throws Exception {
		decompile("multi", "samples/hello.dex", "samples/HelloWorld.smali");
	}

	@Test
	public void testFallbackMode() throws Exception {
		Path tempDir = FileUtils.createTempDir("fallback");
		List<String> args = buildArgs(tempDir, "samples/hello.dex");
		args.add(0, "-f");

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(tempDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testSimpleMode() throws Exception {
		Path tempDir = FileUtils.createTempDir("simple");
		List<String> args = buildArgs(tempDir, "samples/hello.dex");
		args.add(0, "--decompilation-mode");
		args.add(1, "simple");

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectJavaFilesInDir(tempDir);
		assertThat(files).hasSize(1);
	}

	@Test
	public void testResourceOnly() throws Exception {
		Path tempDir = FileUtils.createTempDir("resourceOnly");
		List<String> args = buildArgs(tempDir, "samples/resources-only.apk");

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> files = collectFilesInDir(tempDir,
				path -> path.getFileName().toString().equalsIgnoreCase("AndroidManifest.xml"));
		assertThat(files).isNotEmpty();
	}

	private void decompile(String tmpDirName, String... inputSamples) throws URISyntaxException, IOException {
		Path tempDir = FileUtils.createTempDir(tmpDirName);
		List<String> args = buildArgs(tempDir, inputSamples);

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> resultJavaFiles = collectJavaFilesInDir(tempDir);
		assertThat(resultJavaFiles).isNotEmpty();

		// do not copy input files as resources
		for (Path path : collectFilesInDir(tempDir, LOG_ALL_FILES)) {
			for (String inputSample : inputSamples) {
				assertThat(path.toAbsolutePath().toString()).doesNotContain(inputSample);
			}
		}
	}

	private List<String> buildArgs(Path tempDir, String... inputSamples) throws URISyntaxException {
		List<String> args = new ArrayList<>();
		args.add("-v");
		args.add("-d");
		args.add(tempDir.toAbsolutePath().toString());

		for (String inputSample : inputSamples) {
			URL resource = getClass().getClassLoader().getResource(inputSample);
			assertThat(resource).isNotNull();
			String sampleFile = resource.toURI().getRawPath();
			args.add(sampleFile);
		}
		return args;
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

	@AfterAll
	public static void cleanup() {
		FileUtils.clearTempRootDir();
	}
}
