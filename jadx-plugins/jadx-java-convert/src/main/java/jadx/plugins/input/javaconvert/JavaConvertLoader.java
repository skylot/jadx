package jadx.plugins.input.javaconvert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaConvertLoader {
	private static final Logger LOG = LoggerFactory.getLogger(JavaConvertLoader.class);

	public static ConvertResult process(List<Path> input) {
		ConvertResult result = new ConvertResult();
		for (Path path : input) {
			if (isJavaFile(path)) {
				try {
					convert(result, path);
				} catch (Exception e) {
					LOG.error("Failed to convert file: " + path.toAbsolutePath(), e);
				}
			}
		}
		return result;
	}

	private static boolean isJavaFile(Path path) {
		String fileName = path.getFileName().toString();
		return fileName.endsWith(".jar")
				|| fileName.endsWith(".class");
	}

	private static void convert(ConvertResult result, Path path) throws Exception {
		Path tempDirectory = Files.createTempDirectory("jadx-");
		result.addTempPath(tempDirectory);

		DxConverter.run(path, tempDirectory);

		LOG.debug("Converted to dex: {}", path.toAbsolutePath());
		result.addConvertedFiles(collectFilesInDir(tempDirectory));
	}

	private static List<Path> collectFilesInDir(Path tempDirectory) throws IOException {
		try (Stream<Path> pathStream = Files.walk(tempDirectory)) {
			return pathStream.collect(Collectors.toList());
		}
	}
}
