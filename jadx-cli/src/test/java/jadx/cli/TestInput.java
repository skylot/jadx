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

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.plugins.loader.JadxBasePluginLoader;
import jadx.core.plugins.files.SingleDirFilesGetter;

import static org.assertj.core.api.Assertions.assertThat;

public class TestInput {
	private static final Logger LOG = LoggerFactory.getLogger(TestInput.class);

	private static final PathMatcher LOG_ALL_FILES = path -> {
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

	@Test
	public void testHelp() {
		int result = execJadxCli(new String[] { "--help" });
		assertThat(result).isEqualTo(0);
	}

	@Test
	public void testApkInput() throws Exception {
		int result = execJadxCli(buildArgs(List.of(), "samples/small.apk"));
		assertThat(result).isEqualTo(0);
		List<Path> resultFiles = collectAllFilesInDir(outputDir);
		printFiles(resultFiles);
		assertThat(resultFiles)
				.describedAs("check output files")
				.map(p -> p.getFileName().toString())
				.haveExactly(2, new Condition<>(f -> f.endsWith(".java"), "java classes"))
				.haveExactly(9, new Condition<>(f -> f.endsWith(".xml"), "xml resources"))
				.haveExactly(1, new Condition<>(f -> f.equals("classes.dex"), "dex"))
				.haveExactly(1, new Condition<>(f -> f.equals("AndroidManifest.xml"), "manifest"))
				.hasSize(13);
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

	private void decompile(String... inputSamples) throws URISyntaxException, IOException {
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

	private int execJadxCli(String[] args) {
		return JadxCLI.execute(args, jadxArgs -> {
			// don't use global config and plugins
			jadxArgs.setFilesGetter(new SingleDirFilesGetter(testDir));
			jadxArgs.setPluginLoader(new JadxBasePluginLoader());
		});
	}

	private String[] buildArgs(List<String> options, String... inputSamples) throws URISyntaxException {
		List<String> args = new ArrayList<>(options);
		args.add("-v");
		args.add("-d");
		args.add(outputDir.toAbsolutePath().toString());

		for (String inputSample : inputSamples) {
			URL resource = getClass().getClassLoader().getResource(inputSample);
			assertThat(resource).isNotNull();
			String sampleFile = resource.toURI().getRawPath();
			args.add(sampleFile);
		}
		return args.toArray(new String[0]);
	}

	private void printFiles(List<Path> files) {
		LOG.info("Output files (count: {}):", files.size());
		for (Path file : files) {
			LOG.info(" {}", outputDir.relativize(file));
		}
	}

	private static List<Path> collectJavaFilesInDir(Path dir) throws IOException {
		PathMatcher javaMatcher = dir.getFileSystem().getPathMatcher("glob:**.java");
		return collectFilesInDir(dir, javaMatcher);
	}

	private static List<Path> collectAllFilesInDir(Path dir) throws IOException {
		try (Stream<Path> pathStream = Files.walk(dir)) {
			return pathStream
					.filter(Files::isRegularFile)
					.collect(Collectors.toList());
		}
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
