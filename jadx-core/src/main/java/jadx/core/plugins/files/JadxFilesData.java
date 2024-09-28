package jadx.core.plugins.files;

import java.nio.file.Path;

import jadx.api.plugins.JadxPluginInfo;
import jadx.api.plugins.data.IJadxFiles;

public class JadxFilesData implements IJadxFiles {
	private static final String PLUGINS_DATA_DIR = "plugins-data";

	private final JadxPluginInfo pluginInfo;
	private final IJadxFilesGetter filesGetter;

	public JadxFilesData(JadxPluginInfo pluginInfo, IJadxFilesGetter filesGetter) {
		this.pluginInfo = pluginInfo;
		this.filesGetter = filesGetter;
	}

	@Override
	public Path getPluginCacheDir() {
		return toPluginPath(filesGetter.getCacheDir());
	}

	@Override
	public Path getPluginConfigDir() {
		return toPluginPath(filesGetter.getConfigDir());
	}

	@Override
	public Path getPluginTempDir() {
		return toPluginPath(filesGetter.getTempDir());
	}

	private Path toPluginPath(Path dir) {
		return dir.resolve(PLUGINS_DATA_DIR).resolve(pluginInfo.getPluginId());
	}
}
