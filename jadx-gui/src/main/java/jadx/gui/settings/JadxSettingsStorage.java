package jadx.gui.settings;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.dirs.ProjectDirectories;

import jadx.core.utils.StringUtils;
import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxGUI;

/**
 * Jadx settings storage. Select first available option:
 * 1. json file in system 'config' directory (preferred)
 * 2. using java preferences api: use Windows registry or xml on Linux/Mac (obsolete, load only)
 */
public class JadxSettingsStorage {
	private static final Logger LOG = LoggerFactory.getLogger(JadxSettingsStorage.class);

	private final Path configFile = initConfigFile();

	public synchronized @Nullable String load() throws IOException {
		if (Files.exists(configFile)) {
			return FileUtils.readFile(configFile);
		}
		return null;
	}

	public synchronized void save(String jsonStr) throws IOException {
		FileUtils.writeFile(configFile, jsonStr);
	}

	private static Path initConfigFile() {
		ProjectDirectories jadxDirs = ProjectDirectories.from("io.github", "skylot", "jadx");
		Path confPath = Paths.get(jadxDirs.configDir, "gui.json");
		if (!Files.exists(confPath)) {
			copyFromPreferences(confPath);
		}
		LOG.debug("Using config: {}", confPath);
		return confPath;
	}

	private static void copyFromPreferences(Path confPath) {
		try {
			Preferences prefs = Preferences.userNodeForPackage(JadxGUI.class);
			String str = prefs.get("jadx.gui.settings", "");
			if (StringUtils.notEmpty(str)) {
				FileUtils.writeFile(confPath, str);
				LOG.warn("Settings moved from java preferences to config file: {}", confPath);
			}
		} catch (Exception e) {
			LOG.warn("Failed to load settings from preferences", e);
		}
	}
}
