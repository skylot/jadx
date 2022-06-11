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
	public void testResourceOnly() throws Exception {
		decode("resourceOnly", "samples/resources-only.apk");
	}

	private void decode(String tmpDirName, String apkSample) throws URISyntaxException, IOException {
		List<String> args = new ArrayList<>();
		Path tempDir = FileUtils.createTempDir(tmpDirName);
		args.add("-v");
		args.add("-d");
		args.add(tempDir.toAbsolutePath().toString());

		URL resource = getClass().getClassLoader().getResource(apkSample);
		assertThat(resource).isNotNull();
		String sampleFile = resource.toURI().getRawPath();
		args.add(sampleFile);

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> files = Files.find(
				tempDir,
				3,
				(file, attr) -> file.getFileName().toString().equalsIgnoreCase("AndroidManifest.xml"))
				.collect(Collectors.toList());
		assertThat(files.isEmpty()).isFalse();
	}

	private void decompile(String tmpDirName, String... inputSamples) throws URISyntaxException, IOException {
		List<String> args = new ArrayList<>();
		Path tempDir = FileUtils.createTempDir(tmpDirName);
		args.add("-v");
		args.add("-d");
		args.add(tempDir.toAbsolutePath().toString());

		for (String inputSample : inputSamples) {
			URL resource = getClass().getClassLoader().getResource(inputSample);
			assertThat(resource).isNotNull();
			String sampleFile = resource.toURI().getRawPath();
			args.add(sampleFile);
		}

		int result = JadxCLI.execute(args.toArray(new String[0]));
		assertThat(result).isEqualTo(0);
		List<Path> resultJavaFiles = collectJavaFilesInDir(tempDir);
		assertThat(resultJavaFiles).isNotEmpty();

		// do not copy input files as resources
		PathMatcher logAllFiles = path -> {
			LOG.debug("File in result dir: {}", path);
			return true;
		};
		for (Path path : collectFilesInDir(tempDir, logAllFiles)) {
			for (String inputSample : inputSamples) {
				assertThat(path.toAbsolutePath().toString()).doesNotContain(inputSample);
			}
		}
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
