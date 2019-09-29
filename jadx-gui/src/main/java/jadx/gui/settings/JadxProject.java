package jadx.gui.settings;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.gui.utils.PathTypeAdapter;

public class JadxProject {

	private static final Logger LOG = LoggerFactory.getLogger(JadxProject.class);
	private static final int CURRENT_SETTINGS_VERSION = 0;

	public static final String PROJECT_EXTENSION = "jadx";

	private static final Gson GSON = new GsonBuilder()
			.registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.singleton())
			.create();

	private transient JadxSettings settings;
	private transient String name = "New Project";
	private transient Path projectPath;
	private List<Path> filesPath;
	private List<String[]> treeExpansions = new ArrayList<>();

	private transient boolean saved;
	private transient boolean initial = true;

	private int projectVersion = 0;

	// Don't remove. Used in json serialization
	public JadxProject() {
	}

	public JadxProject(JadxSettings settings) {
		this.settings = settings;
	}

	public void setSettings(JadxSettings settings) {
		this.settings = settings;
	}

	public Path getProjectPath() {
		return projectPath;
	}

	private void setProjectPath(Path projectPath) {
		this.projectPath = projectPath;
		if (projectVersion != CURRENT_SETTINGS_VERSION) {
			upgradeSettings(projectVersion);
		}
		name = projectPath.getFileName().toString();
		name = name.substring(0, name.lastIndexOf('.'));
		changed();
	}

	public Path getFilePath() {
		return filesPath == null ? null : filesPath.get(0);
	}

	public void setFilePath(Path filePath) {
		if (!filePath.equals(getFilePath())) {
			this.filesPath = Arrays.asList(filePath);
			changed();
		}
	}

	public List<String[]> getTreeExpansions() {
		return treeExpansions;
	}

	public void addTreeExpansion(String[] expansion) {
		treeExpansions.add(expansion);
		changed();
	}

	public void removeTreeExpansion(String[] expansion) {
		for (Iterator<String[]> it = treeExpansions.iterator(); it.hasNext();) {
			if (isParentOfExpansion(expansion, it.next())) {
				it.remove();
			}
		}
		changed();
	}

	private boolean isParentOfExpansion(String[] parent, String[] child) {
		if (Arrays.equals(parent, child)) {
			return true;
		}
		for (int i = child.length - parent.length; i > 0; i--) {
			String[] arr = Arrays.copyOfRange(child, i, child.length);
			if (Arrays.equals(parent, arr)) {
				return true;
			}
		}
		return false;
	}

	private void changed() {
		if (settings.isAutoSaveProject()) {
			save();
		} else {
			saved = false;
		}
		initial = false;
	}

	public String getName() {
		return name;
	}

	public boolean isSaved() {
		return saved;
	}

	public boolean isInitial() {
		return initial;
	}

	public void saveAs(Path path) {
		setProjectPath(path);
		save();
	}

	public void save() {
		if (getProjectPath() != null) {
			try (BufferedWriter writer = Files.newBufferedWriter(getProjectPath())) {
				writer.write(GSON.toJson(this));
				saved = true;
			} catch (Exception e) {
				LOG.error("Error saving project", e);
			}
		}
	}

	public static JadxProject from(Path path, JadxSettings settings) {
		try {
			List<String> lines = Files.readAllLines(path);

			if (!lines.isEmpty()) {
				JadxProject project = GSON.fromJson(lines.get(0), JadxProject.class);
				project.settings = settings;
				project.setProjectPath(path);
				project.saved = true;
				return project;
			}
		} catch (Exception e) {
			LOG.error("Error loading project", e);
		}
		return null;
	}

	private void upgradeSettings(int fromVersion) {
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_SETTINGS_VERSION);
		if (fromVersion == 0) {
			fromVersion++;
		}
		projectVersion = CURRENT_SETTINGS_VERSION;
		save();
	}
}
