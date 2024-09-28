package jadx.cli.plugins;

import java.nio.file.Path;

import jadx.commons.app.JadxCommonFiles;
import jadx.core.plugins.files.IJadxFilesGetter;
import jadx.core.utils.files.FileUtils;

public class JadxFilesGetter implements IJadxFilesGetter {

	public static final JadxFilesGetter INSTANCE = new JadxFilesGetter();

	@Override
	public Path getConfigDir() {
		return JadxCommonFiles.getConfigDir();
	}

	@Override
	public Path getCacheDir() {
		return JadxCommonFiles.getCacheDir();
	}

	@Override
	public Path getTempDir() {
		return FileUtils.getTempRootDir();
	}

	private JadxFilesGetter() {
		// singleton
	}
}
