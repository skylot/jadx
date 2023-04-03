package jadx.gui.settings;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import jadx.api.JadxArgs;
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
import jadx.core.utils.files.FileUtils;
import jadx.gui.settings.data.ProjectData;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.utils.RelativePathTypeAdapter;

public class JadxProject {
	private static final Logger LOG = LoggerFactory.getLogger(JadxProject.class);

	private static final int CURRENT_PROJECT_VERSION = 1;
	public static final String PROJECT_EXTENSION = "jadx";

	private static final int SEARCH_HISTORY_LIMIT = 30;

	private final transient MainWindow mainWindow;

	private transient String name = "New Project";
	private transient @Nullable Path projectPath;

	private transient boolean initial = true;
	private transient boolean saved;

	private ProjectData data = new ProjectData();

	public JadxProject(MainWindow mainWindow) {
		this.mainWindow = mainWindow;
	}

	public void fillJadxArgs(JadxArgs jadxArgs) {
		jadxArgs.setInputFiles(FileUtils.toFiles(getFilePaths()));
		jadxArgs.setUserRenamesMappingsPath(getMappingsPath());
		jadxArgs.setCodeData(getCodeData());
		jadxArgs.getPluginOptions().putAll(data.getPluginOptions());
	}

	public @Nullable Path getWorkingDir() {
		if (projectPath != null) {
			return projectPath.toAbsolutePath().getParent();
		}
		List<Path> files = data.getFiles();
		if (!files.isEmpty()) {
			Path path = files.get(0);
			return path.toAbsolutePath().getParent();
		}
		return null;
	}

	@Nullable
	public Path getProjectPath() {
		return projectPath;
	}

	private void setProjectPath(@NotNull Path projectPath) {
		this.projectPath = projectPath;
		this.name = CommonFileUtils.removeFileExtension(projectPath.getFileName().toString());
		changed();
	}

	public List<Path> getFilePaths() {
		return data.getFiles();
	}

	public void setFilePaths(List<Path> files) {
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

	public void saveOpenTabs(List<EditorViewState> tabs) {
		List<TabViewState> tabStateList = tabs.stream()
				.map(TabStateViewAdapter::build)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (tabStateList.isEmpty()) {
			return;
		}
		if (data.setOpenTabs(tabStateList)) {
			changed();
		}
	}

	public List<EditorViewState> getOpenTabs(MainWindow mw) {
		return data.getOpenTabs().stream()
				.map(s -> TabStateViewAdapter.load(mw, s))
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
	}

	public Path getMappingsPath() {
		return data.getMappingsPath();
	}

	public void setMappingsPath(Path mappingsPath) {
		data.setMappingsPath(mappingsPath);
		changed();
	}

	/**
	 * Do not expose options map directly to be able to intercept changes
	 */
	public void updatePluginOptions(Consumer<Map<String, String>> update) {
		update.accept(data.getPluginOptions());
		changed();
	}

	public @Nullable String getPluginOption(String key) {
		return data.getPluginOptions().get(key);
	}

	public @NotNull Path getCacheDir() {
		Path cacheDir = data.getCacheDir();
		if (cacheDir != null) {
			return cacheDir;
		}
		Path newCacheDir = buildCacheDir();
		setCacheDir(newCacheDir);
		return newCacheDir;
	}

	public void setCacheDir(Path cacheDir) {
		data.setCacheDir(cacheDir);
		changed();
	}

	private Path buildCacheDir() {
		if (projectPath != null) {
			return projectPath.resolveSibling(projectPath.getFileName() + ".cache");
		}
		List<Path> files = data.getFiles();
		if (!files.isEmpty()) {
			Path path = files.get(0);
			return path.resolveSibling(path.getFileName() + ".cache");
		}
		throw new JadxRuntimeException("Failed to build cache dir");
	}

	public boolean isEnableLiveReload() {
		return data.isEnableLiveReload();
	}

	public void setEnableLiveReload(boolean newValue) {
		if (newValue != data.isEnableLiveReload()) {
			data.setEnableLiveReload(newValue);
			changed();
		}
	}

	public List<String> getSearchHistory() {
		return data.getSearchHistory();
	}

	public void addToSearchHistory(String str) {
		if (str == null || str.isEmpty()) {
			return;
		}
		List<String> list = data.getSearchHistory();
		if (!list.isEmpty() && list.get(0).equals(str)) {
			return;
		}
		list.remove(str);
		list.add(0, str);
		if (list.size() > SEARCH_HISTORY_LIMIT) {
			list.remove(list.size() - 1);
		}
		data.setSearchHistory(list);
		changed();
	}

	private void changed() {
		JadxSettings settings = mainWindow.getSettings();
		if (settings != null && settings.isAutoSaveProject()) {
			save();
		} else {
			saved = false;
		}
		initial = false;
		mainWindow.updateProject(this);
	}

	public String getName() {
		return name;
	}

	public boolean isSaveFileSelected() {
		return projectPath != null;
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

	public static JadxProject load(MainWindow mainWindow, Path path) {
		Path basePath = path.toAbsolutePath().getParent();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			JadxProject project = new JadxProject(mainWindow);
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
