package jadx.core.plugins.files;

import java.nio.file.Files;
import java.nio.file.Path;

import jadx.core.utils.files.FileUtils;

public class TempFilesGetter implements IJadxFilesGetter {

	public static final TempFilesGetter INSTANCE = new TempFilesGetter();

	private final Path tempRootDir;

	private TempFilesGetter() {
		try {
			tempRootDir = Files.createTempDirectory("jadx-temp-");
			tempRootDir.toFile().deleteOnExit();
		} catch (Exception e) {
			throw new RuntimeException("Failed to create temp directory", e);
		}
	}

	@Override
	public Path getConfigDir() {
		return makeSubDir("config");
	}

	@Override
	public Path getCacheDir() {
		return makeSubDir("cache");
	}

	@Override
	public Path getTempDir() {
		return tempRootDir;
	}

	private Path makeSubDir(String subDir) {
		Path dir = tempRootDir.resolve(subDir);
		FileUtils.makeDirs(dir);
		return dir;
	}
}
