package jadx.gui.settings;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.data.ICodeComment;
import jadx.api.data.IJavaNodeRef;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.utils.GsonUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.PathTypeAdapter;

public class JadxProject {
	private static final Logger LOG = LoggerFactory.getLogger(JadxProject.class);

	private static final int CURRENT_PROJECT_VERSION = 1;
	public static final String PROJECT_EXTENSION = "jadx";

	private static final Gson GSON = new GsonBuilder()
			.registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.singleton())
			.registerTypeAdapter(ICodeComment.class, GsonUtils.interfaceReplace(JadxCodeComment.class))
			.registerTypeAdapter(IJavaNodeRef.class, GsonUtils.interfaceReplace(JadxNodeRef.class))
			.setPrettyPrinting()
			.create();

	private transient MainWindow mainWindow;
	private transient JadxSettings settings;

	private transient String name = "New Project";
	private transient Path projectPath;

	private transient boolean initial = true;
	private transient boolean saved;

	private List<Path> files;
	private List<String[]> treeExpansions = new ArrayList<>();
	private JadxCodeData codeData = new JadxCodeData();

	private int projectVersion;

	public JadxProject() {
	}

	public void setSettings(JadxSettings settings) {
		this.settings = settings;
	}

	public void setMainWindow(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public Path getProjectPath() {
		return projectPath;
	}

	private void setProjectPath(Path projectPath) {
		this.projectPath = projectPath;
		name = projectPath.getFileName().toString();
		int dotPos = name.lastIndexOf('.');
		if (dotPos != -1) {
			name = name.substring(0, dotPos);
		}
		changed();
	}

	public List<Path> getFilePaths() {
		return files;
	}

	public void setFilePath(List<Path> files) {
		if (!files.equals(getFilePaths())) {
			this.files = files;
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
		treeExpansions.removeIf(strings -> isParentOfExpansion(expansion, strings));
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

	public JadxCodeData getCodeData() {
		return codeData;
	}

	public void setCodeData(JadxCodeData codeData) {
		this.codeData = codeData;
		changed();
	}

	private void changed() {
		if (settings != null && settings.isAutoSaveProject()) {
			save();
		} else {
			saved = false;
		}
		initial = false;
		if (mainWindow != null) {
			mainWindow.updateProject(this);
		}
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
			try (Writer writer = Files.newBufferedWriter(getProjectPath(), StandardCharsets.UTF_8)) {
				GSON.toJson(this, writer);
				saved = true;
			} catch (Exception e) {
				LOG.error("Error saving project", e);
			}
		}
	}

	public static JadxProject from(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JadxProject project = GSON.fromJson(reader, JadxProject.class);
			project.saved = true;
			project.setProjectPath(path);
			project.upgrade();
			return project;
		} catch (Exception e) {
			LOG.error("Error loading project", e);
			return null;
		}
	}

	private void upgrade() {
		int fromVersion = projectVersion;
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_PROJECT_VERSION);
		if (fromVersion == 0) {
			fromVersion++;
		}
		if (fromVersion != CURRENT_PROJECT_VERSION) {
			throw new JadxRuntimeException("Project update failed");
		}
		projectVersion = CURRENT_PROJECT_VERSION;
		save();
	}
}
