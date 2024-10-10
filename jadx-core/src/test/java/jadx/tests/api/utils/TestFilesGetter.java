package jadx.tests.api.utils;

import java.nio.file.Path;

import jadx.core.plugins.files.IJadxFilesGetter;
import jadx.core.utils.files.FileUtils;

public class TestFilesGetter implements IJadxFilesGetter {
	private final Path testDir;

	public TestFilesGetter(Path testDir) {
		this.testDir = testDir;
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
		return testDir;
	}

	private Path makeSubDir(String config) {
		Path dir = testDir.resolve(config);
		FileUtils.makeDirs(dir);
		return dir;
	}
}
