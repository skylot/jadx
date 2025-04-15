package jadx.core.plugins.files;

import java.nio.file.Path;

import jadx.core.utils.files.FileUtils;

/**
 * Use single directory for all jadx files
 */
public class SingleDirFilesGetter implements IJadxFilesGetter {
	private final Path baseDir;

	public SingleDirFilesGetter(Path baseDir) {
		this.baseDir = baseDir;
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
		return makeSubDir("temp");
	}

	private Path makeSubDir(String subDir) {
		Path dir = baseDir.resolve(subDir);
		FileUtils.makeDirs(dir);
		return dir;
	}
}
