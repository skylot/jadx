package jadx.cli.plugins;

import java.nio.file.Path;

import jadx.commons.app.JadxCommonFiles;
import jadx.commons.app.JadxTempFiles;
import jadx.core.plugins.files.IJadxFilesGetter;

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
		return JadxTempFiles.getTempRootDir();
	}

	private JadxFilesGetter() {
		// singleton
	}
}
