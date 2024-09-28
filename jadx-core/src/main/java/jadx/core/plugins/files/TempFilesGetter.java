package jadx.core.plugins.files;

import java.nio.file.Path;

import jadx.core.utils.files.FileUtils;

public class TempFilesGetter implements IJadxFilesGetter {

	public static final TempFilesGetter INSTANCE = new TempFilesGetter();

	@Override
	public Path getConfigDir() {
		return FileUtils.getTempRootDir().resolve("config");
	}

	@Override
	public Path getCacheDir() {
		return FileUtils.getTempRootDir().resolve("cache");
	}

	@Override
	public Path getTempDir() {
		return FileUtils.getTempRootDir().resolve("temp");
	}

	private TempFilesGetter() {
		// singleton
	}
}
