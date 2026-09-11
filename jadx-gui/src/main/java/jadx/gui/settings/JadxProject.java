package jadx.gui.settings;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;

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
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.cache.manager.CacheManager;
import jadx.gui.plugins.context.CommonGuiPluginsContext;
import jadx.gui.settings.data.ProjectData;
import jadx.gui.settings.data.SaveOptionEnum;
import jadx.gui.settings.data.TabViewState;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.utils.NLS;
import jadx.gui.utils.RelativePathTypeAdapter;
import jadx.gui.utils.ui.ActionMessageBox;
import jadx.gui.utils.ui.ActionMessageBox.Action;

import static jadx.core.utils.GsonUtils.defaultGsonBuilder;
import static jadx.core.utils.GsonUtils.interfaceReplace;

public class JadxProject {
	public static final String PROJECT_EXTENSION = "jadx";

	private static final int SEARCH_HISTORY_LIMIT = 30;

	private final transient MainWindow mainWindow;
	private final transient TabStateViewAdapter tabStateViewAdapter = new TabStateViewAdapter();

	private transient String name = "New Project";
	private transient String inputsHash = "";
	private transient @Nullable Path projectPath;

	private transient boolean initial = true;
	private transient boolean saved;

	private final ProjectData data;

	public JadxProject(MainWindow mainWindow) {
		this(mainWindow, new ProjectData());
	}

	private JadxProject(MainWindow mainWindow, ProjectData projectData) {
		this.mainWindow = mainWindow;
		this.data = Objects.requireNonNull(projectData);
	}

	public void fillJadxArgs(JadxArgs jadxArgs) {
		jadxArgs.setInputFiles(FileUtils.toFiles(getFilePaths()));
		if (jadxArgs.getUserRenamesMappingsPath() == null) {
			jadxArgs.setUserRenamesMappingsPath(getMappingsPath());
		}
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

	/**
	 * @return null if project not saved
	 */
	public @Nullable Path getProjectPath() {
		return projectPath;
	}

	private void setProjectPath(@NotNull Path projectPath) {
		this.projectPath = projectPath;
		this.name = CommonFileUtils.removeFileExtension(projectPath.getFileName().toString());
	}

	public List<Path> getFilePaths() {
		return data.getFiles();
	}

	public void setFilePaths(List<Path> files) {
		if (files.equals(getFilePaths())) {
			return;
		}
		if (files.isEmpty()) {
			data.setFiles(files);
			name = "";
			inputsHash = "";
		} else {
			List<Path> inputs = verifyInputFiles(files);
			Collections.sort(inputs);
			data.setFiles(inputs);
			name = buildProjectName(inputs);
			inputsHash = FileUtils.buildInputsHash(inputs);
		}
		changed();
	}

	public void verifyFiles() {
		setFilePaths(verifyInputFiles(getFilePaths()));
	}

	private static String buildProjectName(List<Path> files) {
		StringJoiner joiner = new StringJoiner("_");
		for (Path p : files) {
			Path fileNamePart = p.getFileName();
			if (fileNamePart == null) {
				joiner.add(p.toString());
				continue;
			}
			String fileName = fileNamePart.toString();
			if (!fileName.endsWith(".jadx.kts")) {
				joiner.add(CommonFileUtils.removeFileExtension(fileName));
			}
		}
		return StringUtils.abbreviate(joiner.toString(), 100);
	}

	public String getInputsHash() {
		return inputsHash;
	}

	public void setTreeExpansions(List<String> list) {
		if (list.equals(data.getTreeExpansionsV2())) {
			return;
		}
		data.setTreeExpansionsV2(list);
		changed();
	}

	public List<String> getTreeExpansions() {
		return data.getTreeExpansionsV2();
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
				.map(tabStateViewAdapter::build)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if (data.setOpenTabs(tabStateList)) {
			changed();
		}
	}

	public List<EditorViewState> getOpenTabs(MainWindow mw) {
		CommonGuiPluginsContext pluginsContext = mw.getGuiPluginsManager().getPluginsContext();
		tabStateViewAdapter.setCustomAdapters(pluginsContext.getTabStatePersistAdapters());
		return data.getOpenTabs().stream()
				.map(s -> tabStateViewAdapter.load(mw, s))
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

	private Path cacheDir;

	public Path getCacheDir() {
		if (cacheDir == null) {
			cacheDir = resolveCachePath(data.getCacheDir());
		}
		return cacheDir;
	}

	public void resetCacheDir() {
		cacheDir = resolveCachePath(null);
	}

	private Path resolveCachePath(@Nullable String cacheDirStr) {
		CacheManager cacheManager = mainWindow.getCacheManager();
		Path newCacheDir = cacheManager.getCacheDir(this, cacheDirStr);
		String newCacheStr = cacheManager.buildCacheDirStr(newCacheDir);
		if (!newCacheStr.equals(cacheDirStr)) {
			data.setCacheDir(newCacheStr);
			changed();
		}
		return newCacheDir;
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

	public void setSearchResourcesFilter(String searchResourcesFilter) {
		data.setSearchResourcesFilter(searchResourcesFilter);
	}

	public String getSearchResourcesFilter() {
		return data.getSearchResourcesFilter();
	}

	public void setSearchResourcesSizeLimit(int searchResourcesSizeLimit) {
		data.setSearchResourcesSizeLimit(searchResourcesSizeLimit);
	}

	public int getSearchResourcesSizeLimit() {
		return data.getSearchResourcesSizeLimit();
	}

	private void changed() {
		JadxSettings settings = mainWindow.getSettings();
		if (settings != null && settings.getSaveOption() == SaveOptionEnum.ALWAYS) {
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
		mainWindow.getCacheManager().projectPathUpdate(this, path);
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
		ProjectData projectData = loadProjectData(path);
		JadxProject project = new JadxProject(mainWindow, projectData);
		project.saved = true;
		project.setProjectPath(path);
		project.setFilePaths(project.getFilePaths());
		return project;
	}

	public static ProjectData loadProjectData(Path path) {
		Path basePath = path.toAbsolutePath().getParent();
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return buildGson(basePath).fromJson(reader, ProjectData.class);
		} catch (Exception e) {
			throw new JadxRuntimeException("Failed to load project file: " + path, e);
		}
	}

	private List<Path> verifyInputFiles(List<Path> inputFiles) {
		List<Path> files = new ArrayList<>(inputFiles);
		for (Path p : inputFiles) {
			if (!Files.exists(p)) {
				files.remove(p);
				new ActionMessageBox(mainWindow,
						NLS.str("project.dialog_title"),
						NLS.str("project.file_not_found") + ":\n" + p.toAbsolutePath(),
						new Action(NLS.str("project.file_not_found.exclude"), () -> {
							// default action, file already excluded
						}),
						new Action(NLS.str("project.file_not_found.open_another"), () -> {
							files.addAll(new FileDialogWrapper(mainWindow, FileOpenMode.ADD).show());
						})).show();
			}
		}
		if (files.isEmpty()) {
			new ActionMessageBox(mainWindow,
					NLS.str("project.dialog_title"),
					NLS.str("project.empty"),
					new Action(NLS.str("project.empty.add_files"), () -> {
						files.addAll(new FileDialogWrapper(mainWindow, FileOpenMode.ADD).show());
					}),
					new Action(NLS.str("project.empty.close"), () -> {
						// empty project will not open
					})).show();
		}
		return files;
	}

	private static Gson buildGson(Path basePath) {
		return defaultGsonBuilder()
				.registerTypeHierarchyAdapter(Path.class, new RelativePathTypeAdapter(basePath))
				.registerTypeAdapter(ICodeComment.class, interfaceReplace(JadxCodeComment.class))
				.registerTypeAdapter(ICodeRename.class, interfaceReplace(JadxCodeRename.class))
				.registerTypeAdapter(IJavaNodeRef.class, interfaceReplace(JadxNodeRef.class))
				.registerTypeAdapter(IJavaCodeRef.class, interfaceReplace(JadxCodeRef.class))
				.create();
	}
}
