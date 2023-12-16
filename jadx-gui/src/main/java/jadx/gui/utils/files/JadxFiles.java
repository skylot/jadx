package jadx.gui.utils.files;

import java.nio.file.Path;
import java.nio.file.Paths;

import dev.dirs.ProjectDirectories;

import jadx.core.utils.files.FileUtils;

public class JadxFiles {

	private static final ProjectDirectories DIRS = ProjectDirectories.from("io.github", "skylot", "jadx");
	private static final String CONFIG_DIR = DIRS.configDir;

	public static final Path GUI_CONF = Paths.get(CONFIG_DIR, "gui.json");
	public static final Path CACHES_LIST = Paths.get(CONFIG_DIR, "caches.json");

	public static final Path CACHE_DIR = Paths.get(DIRS.cacheDir);
	public static final Path PROJECTS_CACHE_DIR = CACHE_DIR.resolve("projects");

	static {
		FileUtils.makeDirs(Paths.get(CONFIG_DIR));
	}
}
