package jadx.gui.utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FileUtils {
	public static List<Path> toPaths(List<File> files) {
		return files.stream().map(File::toPath).collect(Collectors.toList());
	}

	public static List<Path> toPaths(File[] files) {
		return Stream.of(files).map(File::toPath).collect(Collectors.toList());
	}

	public static List<Path> fileNamesToPaths(List<String> fileNames) {
		return fileNames.stream().map(Paths::get).collect(Collectors.toList());
	}

	public static List<File> toFiles(List<Path> paths) {
		return paths.stream().map(Path::toFile).collect(Collectors.toList());
	}
}
