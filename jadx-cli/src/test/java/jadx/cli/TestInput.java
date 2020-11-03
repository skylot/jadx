package jadx.cli;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
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

	private void decompile(String tmpDirName, String... inputSamples) throws URISyntaxException, IOException {
		StringBuilder args = new StringBuilder();
		Path tempDir = FileUtils.createTempDir(tmpDirName);
		args.append("-v");
		args.append(" -d ").append(tempDir.toAbsolutePath());

		for (String inputSample : inputSamples) {
			URL resource = getClass().getClassLoader().getResource(inputSample);
			assertThat(resource).isNotNull();
			String sampleFile = resource.toURI().getRawPath();
			args.append(' ').append(sampleFile);
		}

		int result = JadxCLI.execute(args.toString().split(" "));
		assertThat(result).isEqualTo(0);
		List<Path> resultJavaFiles = collectJavaFilesInDir(tempDir);
		assertThat(resultJavaFiles).isNotEmpty();
	}

	private static List<Path> collectJavaFilesInDir(Path dir) throws IOException {
		PathMatcher matcher = dir.getFileSystem().getPathMatcher("glob:**.java");
		try (Stream<Path> pathStream = Files.walk(dir)) {
			return pathStream
					.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
					.peek(f -> LOG.debug("File in result dir: {}", f))
					.filter(matcher::matches)
					.collect(Collectors.toList());
		}
	}

	@AfterAll
	public static void cleanup() {
		FileUtils.clearTempRootDir();
	}
}
