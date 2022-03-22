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

import org.apache.commons.lang3.StringUtils;
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
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.utils.GsonUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.settings.data.ProjectData;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.RelativePathTypeAdapter;

public class JadxProject {
	private static final Logger LOG = LoggerFactory.getLogger(JadxProject.class);

	private static final int CURRENT_PROJECT_VERSION = 1;
	public static final String PROJECT_EXTENSION = "jadx";

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
		this.name = CommonFileUtils.removeFileExtension(projectPath.getFileName().toString());
		changed();
	}

	public List<Path> getFilePaths() {
		return data.getFiles();
	}

	public void setFilePath(List<Path> files) {
		if (!files.equals(getFilePaths())) {
			data.setFiles(files);
			String joinedName = files.stream().map(p -> CommonFileUtils.removeFileExtension(p.getFileName().toString()))
					.collect(Collectors.joining("_"));
			this.name = StringUtils.abbreviate(joinedName, 100);
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
		boolean dataChanged;
		dataChanged = data.setOpenTabs(tabStateList);
		dataChanged |= data.setActiveTab(activeTab);
		if (dataChanged) {
			changed();
		}
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
		Path savePath = getProjectPath();
		if (savePath != null) {
			Path basePath = savePath.toAbsolutePath().getParent();
			try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
				buildGson(basePath).toJson(data, writer);
				saved = true;
			} catch (Exception e) {
				throw new RuntimeException("Error saving project", e);
			}
		}
	}

	public static JadxProject from(Path path) {
		Path basePath = path.toAbsolutePath().getParent();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JadxProject project = new JadxProject();
			project.data = buildGson(basePath).fromJson(reader, ProjectData.class);
			project.saved = true;
			project.setProjectPath(path);
			project.upgrade();
			return project;
		} catch (Exception e) {
			LOG.error("Error loading project", e);
			return null;
		}
	}

	private static Gson buildGson(Path basePath) {
		return new GsonBuilder()
				.registerTypeHierarchyAdapter(Path.class, new RelativePathTypeAdapter(basePath))
				.registerTypeAdapter(ICodeComment.class, GsonUtils.interfaceReplace(JadxCodeComment.class))
				.registerTypeAdapter(ICodeRename.class, GsonUtils.interfaceReplace(JadxCodeRename.class))
				.registerTypeAdapter(IJavaNodeRef.class, GsonUtils.interfaceReplace(JadxNodeRef.class))
				.registerTypeAdapter(IJavaCodeRef.class, GsonUtils.interfaceReplace(JadxCodeRef.class))
				.setPrettyPrinting()
				.create();
	}

	private void upgrade() {
		int fromVersion = data.getProjectVersion();
		if (fromVersion == CURRENT_PROJECT_VERSION) {
			return;
		}
		LOG.debug("upgrade project settings from version: {} to {}", fromVersion, CURRENT_PROJECT_VERSION);
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
