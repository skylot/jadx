package jadx.gui.settings;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.data.ICodeComment;
import jadx.api.data.ICodeRename;
import jadx.api.data.IJavaCodeRef;
import jadx.api.data.IJavaNodeRef;
import jadx.api.data.impl.JadxCodeComment;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.utils.GsonUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.settings.data.ProjectData;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.PathTypeAdapter;

public class JadxProject {
	private static final Logger LOG = LoggerFactory.getLogger(JadxProject.class);

	private static final int CURRENT_PROJECT_VERSION = 1;
	public static final String PROJECT_EXTENSION = "jadx";

	private static final Gson GSON = new GsonBuilder()
			.registerTypeHierarchyAdapter(Path.class, PathTypeAdapter.singleton())
			.registerTypeAdapter(ICodeComment.class, GsonUtils.interfaceReplace(JadxCodeComment.class))
			.registerTypeAdapter(ICodeRename.class, GsonUtils.interfaceReplace(JadxCodeRename.class))
			.registerTypeAdapter(IJavaNodeRef.class, GsonUtils.interfaceReplace(JadxNodeRef.class))
			.registerTypeAdapter(IJavaCodeRef.class, GsonUtils.interfaceReplace(JadxCodeRef.class))
			.setPrettyPrinting()
			.create();

	private transient MainWindow mainWindow;
	private transient JadxSettings settings;

	private transient String name = "New Project";
	private transient Path projectPath;

	private transient boolean initial = true;
	private transient boolean saved;

	private ProjectData data = new ProjectData();

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
		this.name = projectPath.getFileName().toString();
		int dotPos = name.lastIndexOf('.');
		if (dotPos != -1) {
			name = name.substring(0, dotPos);
		}
		changed();
	}

	public List<Path> getFilePaths() {
		return data.getFiles();
	}

	public void setFilePath(List<Path> files) {
		if (!files.equals(getFilePaths())) {
			data.setFiles(files);
			changed();
		}
	}

	public List<String[]> getTreeExpansions() {
		return data.getTreeExpansions();
	}

	public void addTreeExpansion(String[] expansion) {
		data.getTreeExpansions().add(expansion);
		changed();
	}

	public void removeTreeExpansion(String[] expansion) {
		data.getTreeExpansions().removeIf(strings -> isParentOfExpansion(expansion, strings));
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
		return data.getCodeData();
	}

	public void setCodeData(JadxCodeData codeData) {
		data.setCodeData(codeData);
		changed();
	}

	public void saveOpenTabs(List<EditorViewState> tabs, int activeTab) {
		List<TabViewState> tabStateList = tabs.stream()
				.map(TabStateViewAdapter::build)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		data.setOpenTabs(tabStateList);
		data.setActiveTab(activeTab);
		changed();
	}

	public List<EditorViewState> getOpenTabs(MainWindow mw) {
		return data.getOpenTabs().stream()
				.map(s -> TabStateViewAdapter.load(mw, s))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public int getActiveTab() {
		return data.getActiveTab();
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
				GSON.toJson(data, writer);
				saved = true;
			} catch (Exception e) {
				LOG.error("Error saving project", e);
			}
		}
	}

	public static JadxProject from(Path path) {
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JadxProject project = new JadxProject();
			project.data = GSON.fromJson(reader, ProjectData.class);
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
		int fromVersion = data.getProjectVersion();
		LOG.debug("upgrade settings from version: {} to {}", fromVersion, CURRENT_PROJECT_VERSION);
		if (fromVersion == 0) {
			fromVersion++;
		}
		if (fromVersion != CURRENT_PROJECT_VERSION) {
			throw new JadxRuntimeException("Project update failed");
		}
		data.setProjectVersion(CURRENT_PROJECT_VERSION);
		save();
	}
}
