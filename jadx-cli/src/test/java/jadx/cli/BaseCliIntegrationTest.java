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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.core.plugins.files.SingleDirFilesGetter;
import jadx.core.utils.Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class BaseCliIntegrationTest {
	private static final Logger LOG = LoggerFactory.getLogger(BaseCliIntegrationTest.class);

	static final PathMatcher LOG_ALL_FILES = path -> {
		LOG.debug("File in result dir: {}", path);
		return true;
	};

	@TempDir
	Path testDir;

	Path outputDir;

	@BeforeEach
	public void setUp() {
		outputDir = testDir.resolve("output");
	}

	int execJadxCli(String sampleName, String... options) {
		return execJadxCli(buildArgs(List.of(options), sampleName));
	}

	int execJadxCli(String[] args) {
		return JadxCLI.execute(args, jadxArgs -> {
			// don't use global config and plugins
			jadxArgs.setFilesGetter(new SingleDirFilesGetter(testDir));
			jadxArgs.setPluginLoader(new JadxBasePluginLoader());
		});
	}

	String[] buildArgs(List<String> options, String... inputSamples) {
		List<String> args = new ArrayList<>(options);
		args.add("-v");
		args.add("-d");
		args.add(outputDir.toAbsolutePath().toString());

		for (String inputSample : inputSamples) {
			try {
				URL resource = getClass().getClassLoader().getResource(inputSample);
				assertThat(resource).isNotNull();
				String sampleFile = resource.toURI().getRawPath();
				args.add(sampleFile);
			} catch (URISyntaxException e) {
				fail("Failed to load sample: " + inputSample, e);
			}
		}
		return args.toArray(new String[0]);
	}

	void decompile(String... inputSamples) throws IOException {
		int result = execJadxCli(buildArgs(List.of(), inputSamples));
		assertThat(result).isEqualTo(0);
		List<Path> resultJavaFiles = collectJavaFilesInDir(outputDir);
		assertThat(resultJavaFiles).isNotEmpty();

		// do not copy input files as resources
		for (Path path : collectFilesInDir(outputDir, LOG_ALL_FILES)) {
			for (String inputSample : inputSamples) {
				assertThat(path.toAbsolutePath().toString()).doesNotContain(inputSample);
			}
		}
	}

	static void printFiles(List<Path> files) {
		LOG.info("Output files (count: {}):", files.size());
		for (Path file : files) {
			LOG.info(" {}", file);
		}
		LOG.info("");
	}

	String pathToUniformString(Path path) {
		return path.toString().replace('\\', '/');
	}

	Path printFileContent(Path file) {
		try {
			String content = Files.readString(outputDir.resolve(file));
			String spacer = Utils.strRepeat("=", 70);
			LOG.info("File content: {}\n{}\n{}\n{}", file, spacer, content, spacer);
			return file;
		} catch (IOException e) {
			throw new RuntimeException("Failed to load file: " + file, e);
		}
	}

	static List<Path> collectJavaFilesInDir(Path dir) throws IOException {
		PathMatcher javaMatcher = dir.getFileSystem().getPathMatcher("glob:**.java");
		return collectFilesInDir(dir, javaMatcher);
	}

	static List<Path> collectAllFilesInDir(Path dir) throws IOException {
		try (Stream<Path> pathStream = Files.walk(dir)) {
			List<Path> files = pathStream
					.filter(Files::isRegularFile)
					.map(dir::relativize)
					.collect(Collectors.toList());
			printFiles(files);
			return files;
		}
	}

	static List<Path> collectFilesInDir(Path dir, PathMatcher matcher) throws IOException {
		try (Stream<Path> pathStream = Files.walk(dir)) {
			List<Path> files = pathStream
					.filter(p -> Files.isRegularFile(p, LinkOption.NOFOLLOW_LINKS))
					.filter(matcher::matches)
					.collect(Collectors.toList());
			printFiles(files);
			return files;
		}
	}
}
