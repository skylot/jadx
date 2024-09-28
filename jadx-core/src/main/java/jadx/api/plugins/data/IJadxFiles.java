package jadx.api.plugins.data;

import java.nio.file.Path;

public interface IJadxFiles {

	/**
	 * Plugin cache directory.
	 */
	Path getPluginCacheDir();

	/**
	 * Plugin config directory.
	 */
	Path getPluginConfigDir();

	/**
	 * Plugin temp directory.
	 */
	Path getPluginTempDir();
}
