package jadx.core.plugins.files;

import java.nio.file.Path;

public interface IJadxFilesGetter {

	Path getConfigDir();

	Path getCacheDir();

	Path getTempDir();
}
