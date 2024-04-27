package jadx.plugins.tools.utils;

import java.nio.file.Path;

import jadx.commons.app.JadxCommonFiles;

import static jadx.core.utils.files.FileUtils.makeDirs;

public class PluginFiles {

	private static final Path PLUGINS_DIR = JadxCommonFiles.getConfigDir().resolve("plugins");
	public static final Path PLUGINS_JSON = PLUGINS_DIR.resolve("plugins.json");
	public static final Path INSTALLED_DIR = PLUGINS_DIR.resolve("installed");
	public static final Path DROPINS_DIR = PLUGINS_DIR.resolve("dropins");

	public static final Path PLUGINS_LIST_CACHE = JadxCommonFiles.getCacheDir().resolve("plugin-list.json");

	static {
		makeDirs(INSTALLED_DIR);
		makeDirs(DROPINS_DIR);
	}
}
