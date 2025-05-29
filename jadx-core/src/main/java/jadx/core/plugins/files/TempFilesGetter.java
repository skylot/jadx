package jadx.core.plugins.files;

import java.nio.file.Files;
import java.nio.file.Path;

import jadx.core.utils.files.FileUtils;

public class TempFilesGetter implements IJadxFilesGetter {

	public static final TempFilesGetter INSTANCE = new TempFilesGetter();

	private static final class TempRootHolder {
		public static final Path TEMP_ROOT_DIR;

		static {
			try {
				TEMP_ROOT_DIR = Files.createTempDirectory("jadx-temp-");
				TEMP_ROOT_DIR.toFile().deleteOnExit();
			} catch (Exception e) {
				throw new RuntimeException("Failed to create temp directory", e);
			}
		}
	}

	private TempFilesGetter() {
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
		return makeSubDir("tmp");
	}

	private Path makeSubDir(String subDir) {
		Path dir = TempRootHolder.TEMP_ROOT_DIR.resolve(subDir);
		FileUtils.makeDirs(dir);
		return dir;
	}
}
