package jadx.plugins.input.javaconvert;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConvertResult implements Closeable {
	private static final Logger LOG = LoggerFactory.getLogger(ConvertResult.class);

	private final List<Path> converted = new ArrayList<>();
	private final List<Path> tmpPaths = new ArrayList<>();

	public List<Path> getConverted() {
		return converted;
	}

	public void addConvertedFiles(List<Path> paths) {
		converted.addAll(paths);
	}

	public void addTempPath(Path path) {
		tmpPaths.add(path);
	}

	public boolean isEmpty() {
		return converted.isEmpty();
	}

	@Override
	public void close() {
		for (Path tmpPath : tmpPaths) {
			try {
				delete(tmpPath);
			} catch (Exception e) {
				LOG.warn("Failed to delete temp path: {}", tmpPath, e);
			}
		}
	}

	@SuppressWarnings("ResultOfMethodCallIgnored")
	private static void delete(Path path) throws IOException {
		if (Files.isRegularFile(path)) {
			Files.delete(path);
			return;
		}
		if (Files.isDirectory(path)) {
			try (Stream<Path> pathStream = Files.walk(path)) {
				pathStream
						.sorted(Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			}
		}
	}

	@Override
	public String toString() {
		return "ConvertResult{converted=" + converted + ", tmpPaths=" + tmpPaths + '}';
	}
}
