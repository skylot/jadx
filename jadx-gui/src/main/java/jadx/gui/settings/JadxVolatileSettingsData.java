package jadx.gui.settings;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import jadx.cli.config.IJadxConfig;

/**
 * Data class to hold volatile jadx-gui settings (recent projects, last paths, window states).
 * Serialized/deserialized separately from {@link JadxSettingsData} via
 * {@link jadx.cli.config.JadxConfigAdapter}.
 */
public class JadxVolatileSettingsData implements IJadxConfig {
	static final Path USER_HOME = Paths.get(System.getProperty("user.home"));

	private Path lastSaveProjectPath = USER_HOME;
	private Path lastOpenFilePath = USER_HOME;
	private Path lastSaveFilePath = USER_HOME;
	private List<Path> recentProjects = new ArrayList<>();

	private Map<String, WindowLocation> windowPos = new HashMap<>();
	private int mainWindowExtendedState = JFrame.NORMAL;

	public Path getLastSaveProjectPath() {
		return lastSaveProjectPath;
	}

	public void setLastSaveProjectPath(Path lastSaveProjectPath) {
		this.lastSaveProjectPath = lastSaveProjectPath;
	}

	public Path getLastOpenFilePath() {
		return lastOpenFilePath;
	}

	public void setLastOpenFilePath(Path lastOpenFilePath) {
		this.lastOpenFilePath = lastOpenFilePath;
	}

	public Path getLastSaveFilePath() {
		return lastSaveFilePath;
	}

	public void setLastSaveFilePath(Path lastSaveFilePath) {
		this.lastSaveFilePath = lastSaveFilePath;
	}

	public List<Path> getRecentProjects() {
		return recentProjects;
	}

	public void setRecentProjects(List<Path> recentProjects) {
		this.recentProjects = recentProjects;
	}

	public Map<String, WindowLocation> getWindowPos() {
		return windowPos;
	}

	public void setWindowPos(Map<String, WindowLocation> windowPos) {
		this.windowPos = windowPos;
	}

	public int getMainWindowExtendedState() {
		return mainWindowExtendedState;
	}

	public void setMainWindowExtendedState(int mainWindowExtendedState) {
		this.mainWindowExtendedState = mainWindowExtendedState;
	}
}
