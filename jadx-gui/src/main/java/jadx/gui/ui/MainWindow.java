package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.WindowConstants;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import jadx.api.JadxArgs;
import jadx.api.JadxDecompiler;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.ResourceFile;
import jadx.api.plugins.events.IJadxEvents;
import jadx.api.plugins.events.JadxEvents;
import jadx.api.plugins.events.types.ReloadProject;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.Jadx;
import jadx.core.export.TemplateFile;
import jadx.core.plugins.events.JadxEventsImpl;
import jadx.core.utils.ListUtils;
import jadx.core.utils.StringUtils;
import jadx.core.utils.android.AndroidManifestParser;
import jadx.core.utils.android.AppAttribute;
import jadx.core.utils.android.ApplicationParams;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.cache.manager.CacheManager;
import jadx.gui.device.debugger.BreakpointManager;
import jadx.gui.events.services.RenameService;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.DecompileTask;
import jadx.gui.jobs.ExportTask;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.logs.LogCollector;
import jadx.gui.logs.LogOptions;
import jadx.gui.logs.LogPanel;
import jadx.gui.plugins.mappings.RenameMappingsGui;
import jadx.gui.plugins.quark.QuarkDialog;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.ui.JadxSettingsWindow;
import jadx.gui.settings.ui.plugins.PluginSettings;
import jadx.gui.treemodel.ApkSignature;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JLoadableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.action.ActionModel;
import jadx.gui.ui.action.JadxGuiAction;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.dialog.ADBDialog;
import jadx.gui.ui.dialog.AboutDialog;
import jadx.gui.ui.dialog.LogViewerDialog;
import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.ui.filedialog.FileDialogWrapper;
import jadx.gui.ui.filedialog.FileOpenMode;
import jadx.gui.ui.menu.HiddenMenuItem;
import jadx.gui.ui.menu.JadxMenu;
import jadx.gui.ui.menu.JadxMenuBar;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.IssuesPanel;
import jadx.gui.ui.panel.JDebuggerPanel;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.ui.popupmenu.RecentProjectsMenuListener;
import jadx.gui.ui.tab.QuickTabsTree;
import jadx.gui.ui.tab.TabbedPane;
import jadx.gui.ui.tab.TabsController;
import jadx.gui.ui.tab.dnd.TabDndController;
import jadx.gui.ui.treenodes.StartPageNode;
import jadx.gui.ui.treenodes.SummaryNode;
import jadx.gui.update.JadxUpdate;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.ILoadListener;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.fileswatcher.LiveReloadWorker;
import jadx.gui.utils.shortcut.ShortcutsController;
import jadx.gui.utils.ui.ActionHandler;
import jadx.gui.utils.ui.NodeLabel;

import static io.reactivex.internal.functions.Functions.EMPTY_RUNNABLE;

public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";

	private static final double BORDER_RATIO = 0.15;
	private static final double WINDOW_RATIO = 1 - BORDER_RATIO * 2;
	public static final double SPLIT_PANE_RESIZE_WEIGHT = 0.15;

	private static final ImageIcon ICON_ADD_FILES = UiUtils.openSvgIcon("ui/addFile");
	private static final ImageIcon ICON_RELOAD = UiUtils.openSvgIcon("ui/refresh");
	private static final ImageIcon ICON_EXPORT = UiUtils.openSvgIcon("ui/export");
	private static final ImageIcon ICON_EXIT = UiUtils.openSvgIcon("ui/exit");
	private static final ImageIcon ICON_SYNC = UiUtils.openSvgIcon("ui/pagination");
	private static final ImageIcon ICON_FLAT_PKG = UiUtils.openSvgIcon("ui/moduleGroup");
	private static final ImageIcon ICON_SEARCH = UiUtils.openSvgIcon("ui/find");
	private static final ImageIcon ICON_FIND = UiUtils.openSvgIcon("ui/ejbFinderMethod");
	private static final ImageIcon ICON_COMMENT_SEARCH = UiUtils.openSvgIcon("ui/usagesFinder");
	private static final ImageIcon ICON_MAIN_ACTIVITY = UiUtils.openSvgIcon("ui/home");
	private static final ImageIcon ICON_BACK = UiUtils.openSvgIcon("ui/left");
	private static final ImageIcon ICON_FORWARD = UiUtils.openSvgIcon("ui/right");
	private static final ImageIcon ICON_QUARK = UiUtils.openSvgIcon("ui/quark");
	private static final ImageIcon ICON_PREF = UiUtils.openSvgIcon("ui/settings");
	private static final ImageIcon ICON_DEOBF = UiUtils.openSvgIcon("ui/helmChartLock");
	private static final ImageIcon ICON_DECOMPILE_ALL = UiUtils.openSvgIcon("ui/runAll");
	private static final ImageIcon ICON_LOG = UiUtils.openSvgIcon("ui/logVerbose");
	private static final ImageIcon ICON_INFO = UiUtils.openSvgIcon("ui/showInfos");
	private static final ImageIcon ICON_DEBUGGER = UiUtils.openSvgIcon("ui/startDebugger");

	private final transient JadxWrapper wrapper;
	private final transient JadxSettings settings;
	private final transient CacheObject cacheObject;
	private final transient CacheManager cacheManager;
	private final transient BackgroundExecutor backgroundExecutor;

	private transient @NotNull JadxProject project;

	private transient JadxGuiAction newProjectAction;
	private transient JadxGuiAction saveProjectAction;

	private transient JPanel mainPanel;
	private transient JSplitPane treeSplitPane;
	private transient JSplitPane rightSplitPane;
	private transient JSplitPane bottomSplitPane;
	private transient JSplitPane quickTabsAndCodeSplitPane;

	private JTree tree;
	private DefaultTreeModel treeModel;
	private JRoot treeRoot;
	private TabsController tabsController;
	private TabbedPane tabbedPane;
	private HeapUsageBar heapUsageBar;
	private transient boolean treeReloading;

	private boolean isFlattenPackage;
	private JToggleButton flatPkgButton;
	private JCheckBoxMenuItem flatPkgMenuItem;

	private JToggleButton deobfToggleBtn;
	private JCheckBoxMenuItem deobfMenuItem;

	private JCheckBoxMenuItem liveReloadMenuItem;
	private final LiveReloadWorker liveReloadWorker;

	private transient Link updateLink;
	private transient ProgressPanel progressPane;
	private transient Theme editorTheme;

	private transient IssuesPanel issuesPanel;
	private transient @Nullable LogPanel logPanel;
	private transient @Nullable JDebuggerPanel debuggerPanel;
	private transient @Nullable QuickTabsTree quickTabsTree;

	private final List<ILoadListener> loadListeners = new ArrayList<>();
	private final List<Consumer<JRoot>> treeUpdateListener = new ArrayList<>();
	private boolean loaded;
	private boolean settingsOpen = false;

	private ShortcutsController shortcutsController;
	private JadxMenuBar menuBar;
	private JMenu pluginsMenu;

	private final transient RenameMappingsGui renameMappings;

	public MainWindow(JadxSettings settings) {
		this.settings = settings;
		this.cacheObject = new CacheObject();
		this.project = new JadxProject(this);
		this.wrapper = new JadxWrapper(this);
		this.liveReloadWorker = new LiveReloadWorker(this);
		this.renameMappings = new RenameMappingsGui(this);
		this.cacheManager = new CacheManager(settings);
		this.shortcutsController = new ShortcutsController(settings);

		JadxEventQueue.register();
		resetCache();
		FontUtils.registerBundledFonts();
		setEditorTheme(settings.getEditorThemePath());
		initUI();
		this.backgroundExecutor = new BackgroundExecutor(settings, progressPane);
		initMenuAndToolbar();
		UiUtils.setWindowIcons(this);
		shortcutsController.registerMouseEventListener(this);
		loadSettings();

		update();
		checkForUpdate();
	}

	public void init() {
		pack();
		setLocationAndPosition();
		treeSplitPane.setDividerLocation(settings.getTreeWidth());
		heapUsageBar.setVisible(settings.isShowHeapUsageBar());
		setVisible(true);
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				closeWindow();
			}
		});

		processCommandLineArgs();
	}

	private void processCommandLineArgs() {
		if (settings.getFiles().isEmpty()) {
			tabbedPane.showNode(new StartPageNode());
		} else {
			open(FileUtils.fileNamesToPaths(settings.getFiles()), this::handleSelectClassOption);
		}
	}

	private void handleSelectClassOption() {
		if (settings.getCmdSelectClass() != null) {
			JavaNode javaNode = wrapper.searchJavaClassByFullAlias(settings.getCmdSelectClass());
			if (javaNode == null) {
				javaNode = wrapper.searchJavaClassByOrigClassName(settings.getCmdSelectClass());
			}
			if (javaNode == null) {
				JOptionPane.showMessageDialog(this,
						NLS.str("msg.cmd_select_class_error", settings.getCmdSelectClass()),
						NLS.str("error_dialog.title"), JOptionPane.ERROR_MESSAGE);
				return;
			}
			tabbedPane.codeJump(cacheObject.getNodeCache().makeFrom(javaNode));
		}
	}

	private void checkForUpdate() {
		if (!settings.isCheckForUpdates()) {
			return;
		}
		JadxUpdate.check(settings.getJadxUpdateChannel(), release -> SwingUtilities.invokeLater(() -> {
			switch (settings.getJadxUpdateChannel()) {
				case STABLE:
					updateLink.setUrl(JadxUpdate.JADX_RELEASES_URL);
					break;
				case UNSTABLE:
					updateLink.setUrl(JadxUpdate.JADX_ARTIFACTS_URL);
					break;
			}
			updateLink.setText(NLS.str("menu.update_label", release.getName()));
			updateLink.setVisible(true);
		}));
	}

	public void openFileDialog() {
		showOpenDialog(FileOpenMode.OPEN);
	}

	public void openProjectDialog() {
		showOpenDialog(FileOpenMode.OPEN_PROJECT);
	}

	private void showOpenDialog(FileOpenMode mode) {
		saveAll();
		if (!ensureProjectIsSaved()) {
			return;
		}
		FileDialogWrapper fileDialog = new FileDialogWrapper(this, mode);
		List<Path> openPaths = fileDialog.show();
		if (!openPaths.isEmpty()) {
			settings.setLastOpenFilePath(fileDialog.getCurrentDir());
			open(openPaths);
		}
	}

	public void addFiles() {
		FileDialogWrapper fileDialog = new FileDialogWrapper(this, FileOpenMode.ADD);
		List<Path> addPaths = fileDialog.show();
		if (!addPaths.isEmpty()) {
			addFiles(addPaths);
		}
	}

	public void addFiles(List<Path> addPaths) {
		project.setFilePaths(ListUtils.distinctMergeSortedLists(addPaths, project.getFilePaths()));
		reopen();
	}

	private void newProject() {
		saveAll();
		if (!ensureProjectIsSaved()) {
			return;
		}
		closeAll();
		updateProject(new JadxProject(this));
	}

	private void saveProject() {
		saveOpenTabs();
		if (!project.isSaveFileSelected()) {
			saveProjectAs();
		} else {
			project.save();
			update();
		}
	}

	private void saveProjectAs() {
		FileDialogWrapper fileDialog = new FileDialogWrapper(this, FileOpenMode.SAVE_PROJECT);
		if (project.getFilePaths().size() == 1) {
			// If there is only one file loaded we suggest saving the jadx project file next to the loaded file
			Path projectPath = getProjectPathForFile(this.project.getFilePaths().get(0));
			fileDialog.setSelectedFile(projectPath);
		}
		List<Path> saveFiles = fileDialog.show();
		if (saveFiles.isEmpty()) {
			return;
		}
		settings.setLastSaveProjectPath(fileDialog.getCurrentDir());
		Path savePath = saveFiles.get(0);
		if (!savePath.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JadxProject.PROJECT_EXTENSION)) {
			savePath = savePath.resolveSibling(savePath.getFileName() + "." + JadxProject.PROJECT_EXTENSION);
		}
		if (Files.exists(savePath)) {
			int res = JOptionPane.showConfirmDialog(
					this,
					NLS.str("confirm.save_as_message", savePath.getFileName()),
					NLS.str("confirm.save_as_title"),
					JOptionPane.YES_NO_OPTION);
			if (res == JOptionPane.NO_OPTION) {
				return;
			}
		}
		project.saveAs(savePath);
		settings.addRecentProject(savePath);
		update();
	}

	public void addNewScript() {
		FileDialogWrapper fileDialog = new FileDialogWrapper(this, FileOpenMode.CUSTOM_SAVE);
		fileDialog.setTitle(NLS.str("file.save"));
		Path workingDir = project.getWorkingDir();
		Path baseDir = workingDir != null ? workingDir : settings.getLastSaveFilePath();
		fileDialog.setSelectedFile(baseDir.resolve("script.jadx.kts"));
		fileDialog.setFileExtList(Collections.singletonList("jadx.kts"));
		fileDialog.setSelectionMode(JFileChooser.FILES_ONLY);
		List<Path> paths = fileDialog.show();
		if (paths.size() != 1) {
			return;
		}
		Path scriptFile = paths.get(0);
		try {
			TemplateFile tmpl = TemplateFile.fromResources("/files/script.jadx.kts.tmpl");
			FileUtils.writeFile(scriptFile, tmpl.build());
		} catch (Exception e) {
			LOG.error("Failed to save new script file: {}", scriptFile, e);
		}
		List<Path> inputs = project.getFilePaths();
		inputs.add(scriptFile);
		project.setFilePaths(inputs);
		project.save();
		reopen();
	}

	public void removeInput(Path file) {
		List<Path> inputs = project.getFilePaths();
		inputs.remove(file);
		project.setFilePaths(inputs);
		project.save();
		reopen();
	}

	public void open(Path path) {
		open(Collections.singletonList(path), EMPTY_RUNNABLE);
	}

	public void open(List<Path> paths) {
		open(paths, EMPTY_RUNNABLE);
	}

	private void open(List<Path> paths, Runnable onFinish) {
		saveAll();
		closeAll();
		if (paths.size() == 1 && openSingleFile(paths.get(0), onFinish)) {
			return;
		}
		// start new project
		project = new JadxProject(this);
		project.setFilePaths(paths);
		loadFiles(onFinish);
	}

	private boolean openSingleFile(Path singleFile, Runnable onFinish) {
		if (singleFile.getFileName() == null) {
			return false;
		}
		String fileExtension = CommonFileUtils.getFileExtension(singleFile.getFileName().toString());
		if (fileExtension != null && fileExtension.equalsIgnoreCase(JadxProject.PROJECT_EXTENSION)) {
			openProject(singleFile, onFinish);
			return true;
		}
		// check if project file already saved with default name
		Path projectPath = getProjectPathForFile(singleFile);
		if (Files.exists(projectPath)) {
			openProject(projectPath, onFinish);
			return true;
		}
		return false;
	}

	private static Path getProjectPathForFile(Path loadedFile) {
		String fileName = loadedFile.getFileName() + "." + JadxProject.PROJECT_EXTENSION;
		return loadedFile.resolveSibling(fileName);
	}

	public void reopen() {
		synchronized (ReloadProject.EVENT) {
			saveAll();
			closeAll();
			loadFiles(EMPTY_RUNNABLE);

			menuBar.reloadShortcuts();
		}
	}

	private void openProject(Path path, Runnable onFinish) {
		LOG.debug("Loading project: {}", path);
		JadxProject jadxProject = JadxProject.load(this, path);
		if (jadxProject == null) {
			JOptionPane.showMessageDialog(
					this,
					NLS.str("msg.project_error"),
					NLS.str("msg.project_error_title"),
					JOptionPane.INFORMATION_MESSAGE);
			jadxProject = new JadxProject(this);
		}
		settings.addRecentProject(path);
		project = jadxProject;
		loadFiles(onFinish);
	}

	private void loadFiles(Runnable onFinish) {
		if (project.getFilePaths().isEmpty()) {
			tabbedPane.showNode(new StartPageNode());
			return;
		}
		AtomicReference<Exception> wrapperException = new AtomicReference<>();
		backgroundExecutor.execute(NLS.str("progress.load"),
				() -> {
					try {
						wrapper.open();
					} catch (Exception e) {
						wrapperException.set(e);
					}
				},
				status -> {
					if (wrapperException.get() != null) {
						closeAll();
						Exception e = wrapperException.get();
						if (e instanceof RuntimeException) {
							throw (RuntimeException) e;
						} else {
							throw new JadxRuntimeException("Project load error", e);
						}
					}
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						showHeapUsageBar();
						UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
						return;
					}
					if (status != TaskStatus.COMPLETE) {
						LOG.warn("Loading task incomplete, status: {}", status);
						return;
					}
					checkLoadedStatus();
					onOpen();
					onFinish.run();
				});
	}

	private void saveAll() {
		saveOpenTabs();
		BreakpointManager.saveAndExit();
	}

	private void closeAll() {
		notifyLoadListeners(false);
		cancelBackgroundJobs();
		clearTree();
		resetCache();
		LogCollector.getInstance().reset();
		wrapper.close();
		tabsController.forceCloseAllTabs();
		UiUtils.resetClipboardOwner();
		System.gc();
		update();
	}

	private void checkLoadedStatus() {
		if (!wrapper.getClasses().isEmpty()) {
			return;
		}
		int errors = issuesPanel.getErrorsCount();
		if (errors > 0) {
			int result = JOptionPane.showConfirmDialog(this,
					NLS.str("message.load_errors", errors),
					NLS.str("message.errorTitle"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.ERROR_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
				showLogViewer(LogOptions.allWithLevel(Level.ERROR));
			}
		} else {
			UiUtils.showMessageBox(this, NLS.str("message.no_classes"));
		}
	}

	private void onOpen() {
		initTree();
		updateLiveReload(project.isEnableLiveReload());
		BreakpointManager.init(project.getFilePaths().get(0).toAbsolutePath().getParent());
		initEvents();

		List<EditorViewState> openTabs = project.getOpenTabs(this);
		backgroundExecutor.execute(NLS.str("progress.load"),
				() -> preLoadOpenTabs(openTabs),
				status -> {
					restoreOpenTabs(openTabs);
					runInitialBackgroundJobs();
					notifyLoadListeners(true);
					update();
				});
	}

	public void passesReloaded() {
		initEvents(); // TODO: events reset on reload passes on script run
		tabbedPane.reloadInactiveTabs();
		reloadTree();
	}

	private void initEvents() {
		events().addListener(JadxEvents.RELOAD_PROJECT, ev -> UiUtils.uiRun(this::reopen));
		RenameService.init(this);
	}

	public void updateLiveReload(boolean state) {
		if (liveReloadWorker.isStarted() == state) {
			return;
		}
		project.setEnableLiveReload(state);
		liveReloadMenuItem.setEnabled(false);
		backgroundExecutor.execute(
				(state ? "Starting" : "Stopping") + " live reload",
				() -> liveReloadWorker.updateState(state),
				s -> {
					liveReloadMenuItem.setState(state);
					liveReloadMenuItem.setEnabled(true);
				});
	}

	private void addTreeCustomNodes() {
		treeRoot.replaceCustomNode(ApkSignature.getApkSignature(wrapper));
		treeRoot.replaceCustomNode(new SummaryNode(this));
	}

	private boolean ensureProjectIsSaved() {
		if (!project.isSaved() && !project.isInitial()) {
			// Check if we saved settings that indicate what to do

			if (settings.getSaveOption() == JadxSettings.SAVEOPTION.NEVER) {
				return true;
			}

			if (settings.getSaveOption() == JadxSettings.SAVEOPTION.ALWAYS) {
				saveProject();
				return true;
			}

			JCheckBox remember = new JCheckBox(NLS.str("confirm.remember"));
			JLabel message = new JLabel(NLS.str("confirm.not_saved_message"));

			JPanel inner = new JPanel(new BorderLayout());
			inner.add(remember, BorderLayout.SOUTH);
			inner.add(message, BorderLayout.NORTH);

			int res = JOptionPane.showConfirmDialog(
					this,
					inner,
					NLS.str("confirm.not_saved_title"),
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (res == JOptionPane.CANCEL_OPTION) {
				return false;
			}
			if (res == JOptionPane.YES_OPTION) {
				if (remember.isSelected()) {
					settings.setSaveOption(JadxSettings.SAVEOPTION.ALWAYS);
					settings.sync();
				}
				saveProject();
			} else if (res == JOptionPane.NO_OPTION) {
				if (remember.isSelected()) {
					settings.setSaveOption(JadxSettings.SAVEOPTION.NEVER);
					settings.sync();
				}
			}
		}
		return true;
	}

	public void updateProject(@NotNull JadxProject jadxProject) {
		this.project = jadxProject;
		UiUtils.uiRun(this::update);
	}

	public void update() {
		UiUtils.uiThreadGuard();
		newProjectAction.setEnabled(!project.isInitial());
		saveProjectAction.setEnabled(loaded && !project.isSaved());
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		renameMappings.onUpdate(loaded);

		Path projectPath = project.getProjectPath();
		String pathString;
		if (projectPath == null) {
			pathString = "";
		} else {
			pathString = " [" + projectPath.toAbsolutePath().getParent() + ']';
		}
		setTitle((project.isSaved() ? "" : '*')
				+ project.getName() + pathString + " - " + DEFAULT_TITLE);
	}

	protected void resetCache() {
		cacheObject.reset();
	}

	synchronized void runInitialBackgroundJobs() {
		if (settings.isAutoStartJobs()) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					requestFullDecompilation();
				}
			}, 1000);
		}
	}

	public void requestFullDecompilation() {
		if (cacheObject.isFullDecompilationFinished()) {
			return;
		}
		backgroundExecutor.execute(new DecompileTask(this));
	}

	public void resetCodeCache() {
		backgroundExecutor.execute(
				NLS.str("preferences.cache.task.delete"),
				() -> {
					try {
						getWrapper().getCurrentDecompiler().ifPresent(jadx -> {
							try {
								jadx.getArgs().getCodeCache().close();
							} catch (Exception e) {
								LOG.error("Failed to close code cache", e);
							}
						});
						Path cacheDir = project.getCacheDir();
						project.resetCacheDir();
						FileUtils.deleteDirIfExists(cacheDir);
					} catch (Exception e) {
						LOG.error("Error during code cache reset", e);
					}
				},
				status -> events().send(ReloadProject.EVENT));
	}

	public void cancelBackgroundJobs() {
		backgroundExecutor.cancelAll();
	}

	private void saveAll(boolean export) {
		FileDialogWrapper fileDialog = new FileDialogWrapper(this, FileOpenMode.EXPORT);
		List<Path> saveDirs = fileDialog.show();
		if (saveDirs.isEmpty()) {
			return;
		}
		JadxArgs decompilerArgs = wrapper.getArgs();
		decompilerArgs.setExportAsGradleProject(export);
		if (export) {
			decompilerArgs.setSkipSources(false);
			decompilerArgs.setSkipResources(false);
		} else {
			decompilerArgs.setSkipSources(settings.isSkipSources());
			decompilerArgs.setSkipResources(settings.isSkipResources());
		}
		settings.setLastSaveFilePath(fileDialog.getCurrentDir());
		backgroundExecutor.execute(new ExportTask(this, wrapper, saveDirs.get(0).toFile()));
	}

	public void initTree() {
		treeRoot = new JRoot(wrapper);
		treeRoot.setFlatPackages(isFlattenPackage);
		treeModel.setRoot(treeRoot);
		addTreeCustomNodes();
		treeRoot.update();
		reloadTree();
	}

	private void clearTree() {
		tabbedPane.reset();
		treeRoot = null;
		treeModel.setRoot(null);
		treeModel.reload();
	}

	public void reloadTree() {
		treeReloading = true;
		treeUpdateListener.forEach(listener -> listener.accept(treeRoot));

		treeModel.reload();
		List<String[]> treeExpansions = project.getTreeExpansions();
		if (!treeExpansions.isEmpty()) {
			expand(treeRoot, treeExpansions);
		} else {
			tree.expandRow(1);
		}

		treeReloading = false;
	}

	public void rebuildPackagesTree() {
		cacheObject.setPackageHelper(null);
		treeRoot.update();
	}

	private void expand(TreeNode node, List<String[]> treeExpansions) {
		TreeNode[] pathNodes = treeModel.getPathToRoot(node);
		if (pathNodes == null) {
			return;
		}
		TreePath path = new TreePath(pathNodes);
		for (String[] expansion : treeExpansions) {
			if (Arrays.equals(expansion, getPathExpansion(path))) {
				tree.expandPath(path);
				break;
			}
		}
		for (int i = node.getChildCount() - 1; i >= 0; i--) {
			expand(node.getChildAt(i), treeExpansions);
		}
	}

	private void toggleFlattenPackage() {
		setFlattenPackage(!isFlattenPackage);
	}

	private void setFlattenPackage(boolean value) {
		isFlattenPackage = value;
		settings.setFlattenPackage(isFlattenPackage);

		flatPkgButton.setSelected(isFlattenPackage);
		flatPkgMenuItem.setState(isFlattenPackage);

		Object root = treeModel.getRoot();
		if (root instanceof JRoot) {
			JRoot treeRoot = (JRoot) root;
			treeRoot.setFlatPackages(isFlattenPackage);
			reloadTree();
		}
	}

	private void toggleDeobfuscation() {
		boolean deobfOn = !settings.isDeobfuscationOn();
		settings.setDeobfuscationOn(deobfOn);
		settings.sync();

		deobfToggleBtn.setSelected(deobfOn);
		deobfMenuItem.setState(deobfOn);
		reopen();
	}

	private boolean nodeClickAction(@Nullable Object obj) {
		if (obj == null) {
			return false;
		}
		try {
			if (obj instanceof JResource) {
				JResource res = (JResource) obj;
				ResourceFile resFile = res.getResFile();
				if (resFile != null && JResource.isSupportedForView(resFile.getType())) {
					return tabbedPane.showNode(res);
				}
			} else if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				if (node.getRootClass() != null) {
					tabbedPane.codeJump(node);
					return true;
				}
				return tabbedPane.showNode(node);
			}
		} catch (Exception e) {
			LOG.error("Content loading error", e);
		}
		return false;
	}

	private void treeRightClickAction(MouseEvent e) {
		JNode node = getJNodeUnderMouse(e);
		if (node == null) {
			return;
		}
		JPopupMenu menu = node.onTreePopupMenu(this);
		if (menu != null) {
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Nullable
	private JNode getJNodeUnderMouse(MouseEvent mouseEvent) {
		TreeNode treeNode = UiUtils.getTreeNodeUnderMouse(tree, mouseEvent);
		if (treeNode instanceof JNode) {
			return (JNode) treeNode;
		}

		return null;
	}

	public void syncWithEditor() {
		ContentPanel selectedContentPanel = tabbedPane.getSelectedContentPanel();
		if (selectedContentPanel == null) {
			return;
		}
		JNode node = selectedContentPanel.getNode();
		if (node.getParent() == null && treeRoot != null) {
			// node not register in tree
			node = treeRoot.searchNode(node);
			if (node == null) {
				LOG.error("Class not found in tree");
				return;
			}
		}
		TreeNode[] pathNodes = treeModel.getPathToRoot(node);
		if (pathNodes == null) {
			return;
		}
		TreePath path = new TreePath(pathNodes);
		tree.setSelectionPath(path);
		tree.makeVisible(path);
		tree.scrollPathToVisible(path);
		tree.requestFocus();
	}

	public void textSearch() {
		ContentPanel panel = tabbedPane.getSelectedContentPanel();
		if (panel instanceof AbstractCodeContentPanel) {
			AbstractCodeArea codeArea = ((AbstractCodeContentPanel) panel).getCodeArea();
			String preferText = codeArea.getSelectedText();
			if (StringUtils.isEmpty(preferText)) {
				preferText = codeArea.getWordUnderCaret();
			}
			if (!StringUtils.isEmpty(preferText)) {
				SearchDialog.searchText(MainWindow.this, preferText);
				return;
			}
		}
		SearchDialog.search(MainWindow.this, SearchDialog.SearchPreset.TEXT);
	}

	public void goToMainActivity() {
		AndroidManifestParser parser = new AndroidManifestParser(
				AndroidManifestParser.getAndroidManifest(getWrapper().getResources()),
				EnumSet.of(AppAttribute.MAIN_ACTIVITY));
		if (!parser.isManifestFound()) {
			JOptionPane.showMessageDialog(MainWindow.this,
					NLS.str("error_dialog.not_found", "AndroidManifest.xml"),
					NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			ApplicationParams results = parser.parse();
			if (results.getMainActivity() == null) {
				throw new JadxRuntimeException("Failed to get main activity name from manifest");
			}
			JavaClass mainActivityClass = results.getMainActivityJavaClass(getWrapper().getDecompiler());
			if (mainActivityClass == null) {
				throw new JadxRuntimeException("Failed to find main activity class: " + results.getMainActivity());
			}
			tabbedPane.codeJump(getCacheObject().getNodeCache().makeFrom(mainActivityClass));
		} catch (Exception e) {
			LOG.error("Main activity not found", e);
			JOptionPane.showMessageDialog(MainWindow.this,
					NLS.str("error_dialog.not_found", "Main Activity"),
					NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	public void goToApplication() {
		AndroidManifestParser parser = new AndroidManifestParser(
				AndroidManifestParser.getAndroidManifest(getWrapper().getResources()),
				EnumSet.of(AppAttribute.APPLICATION));
		if (!parser.isManifestFound()) {
			JOptionPane.showMessageDialog(MainWindow.this,
					NLS.str("error_dialog.not_found", "AndroidManifest.xml"),
					NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		try {
			ApplicationParams results = parser.parse();
			if (results.getApplication() == null) {
				throw new JadxRuntimeException("Failed to get application from manifest");
			}
			JavaClass applicationClass = results.getApplicationJavaClass(getWrapper().getDecompiler());
			if (applicationClass == null) {
				throw new JadxRuntimeException("Failed to find application class: " + results.getApplication());
			}
			tabbedPane.codeJump(getCacheObject().getNodeCache().makeFrom(applicationClass));
		} catch (Exception e) {
			LOG.error("Application not found", e);
			JOptionPane.showMessageDialog(MainWindow.this,
					NLS.str("error_dialog.not_found", "Application"),
					NLS.str("error_dialog.title"),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void initMenuAndToolbar() {
		JadxGuiAction openAction = new JadxGuiAction(ActionModel.OPEN, this::openFileDialog);
		JadxGuiAction openProject = new JadxGuiAction(ActionModel.OPEN_PROJECT, this::openProjectDialog);

		JadxGuiAction addFilesAction = new JadxGuiAction(ActionModel.ADD_FILES, () -> addFiles());
		newProjectAction = new JadxGuiAction(ActionModel.NEW_PROJECT, this::newProject);
		saveProjectAction = new JadxGuiAction(ActionModel.SAVE_PROJECT, this::saveProject);
		JadxGuiAction saveProjectAsAction = new JadxGuiAction(ActionModel.SAVE_PROJECT_AS, this::saveProjectAs);
		JadxGuiAction reloadAction = new JadxGuiAction(ActionModel.RELOAD, () -> UiUtils.uiRun(this::reopen));
		JadxGuiAction liveReloadAction = new JadxGuiAction(ActionModel.LIVE_RELOAD,
				() -> updateLiveReload(!project.isEnableLiveReload()));

		liveReloadMenuItem = new JCheckBoxMenuItem(liveReloadAction);
		liveReloadMenuItem.setState(project.isEnableLiveReload());

		JadxGuiAction saveAllAction = new JadxGuiAction(ActionModel.SAVE_ALL, () -> saveAll(false));
		JadxGuiAction exportAction = new JadxGuiAction(ActionModel.EXPORT, () -> saveAll(true));

		JMenu recentProjects = new JadxMenu(NLS.str("menu.recent_projects"), shortcutsController);
		recentProjects.addMenuListener(new RecentProjectsMenuListener(this, recentProjects));

		JadxGuiAction prefsAction = new JadxGuiAction(ActionModel.PREFS, this::openSettings);
		JadxGuiAction exitAction = new JadxGuiAction(ActionModel.EXIT, this::closeWindow);

		isFlattenPackage = settings.isFlattenPackage();
		flatPkgMenuItem = new JCheckBoxMenuItem(NLS.str("menu.flatten"), ICON_FLAT_PKG);
		flatPkgMenuItem.setState(isFlattenPackage);

		JCheckBoxMenuItem heapUsageBarMenuItem = new JCheckBoxMenuItem(NLS.str("menu.heapUsageBar"));
		heapUsageBarMenuItem.setState(settings.isShowHeapUsageBar());
		heapUsageBarMenuItem.addActionListener(event -> {
			settings.setShowHeapUsageBar(!settings.isShowHeapUsageBar());
			heapUsageBar.setVisible(settings.isShowHeapUsageBar());
		});

		JCheckBoxMenuItem alwaysSelectOpened = new JCheckBoxMenuItem(NLS.str("menu.alwaysSelectOpened"));
		alwaysSelectOpened.setState(settings.isAlwaysSelectOpened());
		alwaysSelectOpened.addActionListener(event -> {
			settings.setAlwaysSelectOpened(!settings.isAlwaysSelectOpened());
			if (settings.isAlwaysSelectOpened()) {
				this.syncWithEditor();
			}
		});

		JCheckBoxMenuItem dockLog = new JCheckBoxMenuItem(NLS.str("menu.dock_log"));
		dockLog.setState(settings.isDockLogViewer());
		dockLog.addActionListener(event -> settings.setDockLogViewer(!settings.isDockLogViewer()));

		JCheckBoxMenuItem dockQuickTabs = new JCheckBoxMenuItem(NLS.str("menu.dock_quick_tabs"));
		dockQuickTabs.setState(settings.isDockQuickTabs());
		dockQuickTabs.addActionListener(event -> {
			boolean visible = quickTabsTree == null;
			setQuickTabsVisibility(visible);
			settings.setDockQuickTabs(visible);
		});
		if (dockQuickTabs.getState()) {
			setQuickTabsVisibility(true);
		}

		JadxGuiAction syncAction = new JadxGuiAction(ActionModel.SYNC, this::syncWithEditor);
		JadxGuiAction textSearchAction = new JadxGuiAction(ActionModel.TEXT_SEARCH, this::textSearch);
		JadxGuiAction clsSearchAction = new JadxGuiAction(ActionModel.CLASS_SEARCH,
				() -> SearchDialog.search(MainWindow.this, SearchDialog.SearchPreset.CLASS));
		JadxGuiAction commentSearchAction = new JadxGuiAction(ActionModel.COMMENT_SEARCH,
				() -> SearchDialog.search(MainWindow.this, SearchDialog.SearchPreset.COMMENT));
		JadxGuiAction goToMainActivityAction = new JadxGuiAction(ActionModel.GO_TO_MAIN_ACTIVITY,
				this::goToMainActivity);
		JadxGuiAction goToApplicationAction = new JadxGuiAction(ActionModel.GO_TO_APPLICATION,
				this::goToApplication);
		JadxGuiAction decompileAllAction = new JadxGuiAction(ActionModel.DECOMPILE_ALL, this::requestFullDecompilation);
		JadxGuiAction resetCacheAction = new JadxGuiAction(ActionModel.RESET_CACHE, this::resetCodeCache);
		JadxGuiAction deobfAction = new JadxGuiAction(ActionModel.DEOBF, this::toggleDeobfuscation);

		deobfToggleBtn = new JToggleButton(deobfAction);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		deobfToggleBtn.setText("");

		deobfMenuItem = new JCheckBoxMenuItem(deobfAction);
		deobfMenuItem.setState(settings.isDeobfuscationOn());

		JadxGuiAction showLogAction = new JadxGuiAction(ActionModel.SHOW_LOG,
				() -> showLogViewer(LogOptions.current()));
		JadxGuiAction aboutAction = new JadxGuiAction(ActionModel.ABOUT, () -> new AboutDialog().setVisible(true));
		JadxGuiAction backAction = new JadxGuiAction(ActionModel.BACK, tabbedPane::navBack);
		JadxGuiAction backVariantAction = new JadxGuiAction(ActionModel.BACK_V, tabbedPane::navBack);
		JadxGuiAction forwardAction = new JadxGuiAction(ActionModel.FORWARD, tabbedPane::navForward);
		JadxGuiAction forwardVariantAction = new JadxGuiAction(ActionModel.FORWARD_V, tabbedPane::navForward);
		JadxGuiAction quarkAction = new JadxGuiAction(ActionModel.QUARK,
				() -> new QuarkDialog(MainWindow.this).setVisible(true));
		JadxGuiAction openDeviceAction = new JadxGuiAction(ActionModel.OPEN_DEVICE,
				() -> new ADBDialog(MainWindow.this).setVisible(true));

		JMenu file = new JadxMenu(NLS.str("menu.file"), shortcutsController);
		file.setMnemonic(KeyEvent.VK_F);
		file.add(openAction);
		file.add(openProject);
		file.add(addFilesAction);
		file.addSeparator();
		file.add(newProjectAction);
		file.add(saveProjectAction);
		file.add(saveProjectAsAction);
		file.addSeparator();
		file.add(reloadAction);
		file.add(liveReloadMenuItem);
		renameMappings.addMenuActions(file);
		file.addSeparator();
		file.add(saveAllAction);
		file.add(exportAction);
		file.addSeparator();
		file.add(recentProjects);
		file.addSeparator();
		file.add(prefsAction);
		file.addSeparator();
		file.add(exitAction);

		JMenu view = new JadxMenu(NLS.str("menu.view"), shortcutsController);
		view.setMnemonic(KeyEvent.VK_V);
		view.add(flatPkgMenuItem);
		view.add(syncAction);
		view.add(heapUsageBarMenuItem);
		view.add(alwaysSelectOpened);
		view.add(dockLog);
		view.add(dockQuickTabs);

		JMenu nav = new JadxMenu(NLS.str("menu.navigation"), shortcutsController);
		nav.setMnemonic(KeyEvent.VK_N);
		nav.add(textSearchAction);
		nav.add(clsSearchAction);
		nav.add(commentSearchAction);
		nav.add(goToMainActivityAction);
		nav.add(goToApplicationAction);
		nav.addSeparator();
		nav.add(backAction);
		nav.add(forwardAction);

		pluginsMenu = new JadxMenu(NLS.str("menu.plugins"), shortcutsController);
		pluginsMenu.setMnemonic(KeyEvent.VK_P);
		resetPluginsMenu();

		JMenu tools = new JadxMenu(NLS.str("menu.tools"), shortcutsController);
		tools.setMnemonic(KeyEvent.VK_T);
		tools.add(decompileAllAction);
		tools.add(resetCacheAction);
		tools.add(deobfMenuItem);
		tools.add(quarkAction);
		tools.add(openDeviceAction);

		JMenu help = new JadxMenu(NLS.str("menu.help"), shortcutsController);
		help.setMnemonic(KeyEvent.VK_H);
		help.add(showLogAction);
		if (Jadx.isDevVersion()) {
			help.add(new AbstractAction("Show sample error report") {
				@Override
				public void actionPerformed(ActionEvent e) {
					ExceptionDialog.throwTestException();
				}
			});
		}
		help.add(aboutAction);

		menuBar = new JadxMenuBar();
		menuBar.add(file);
		menuBar.add(view);
		menuBar.add(nav);
		menuBar.add(tools);
		menuBar.add(pluginsMenu);
		menuBar.add(help);
		setJMenuBar(menuBar);

		flatPkgButton = new JToggleButton(ICON_FLAT_PKG);
		flatPkgButton.setSelected(isFlattenPackage);
		ActionListener flatPkgAction = e -> toggleFlattenPackage();
		flatPkgMenuItem.addActionListener(flatPkgAction);
		flatPkgButton.addActionListener(flatPkgAction);
		flatPkgButton.setToolTipText(NLS.str("menu.flatten"));

		updateLink = new Link();
		updateLink.setVisible(false);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(openAction);
		toolbar.add(addFilesAction);
		toolbar.addSeparator();
		toolbar.add(reloadAction);
		toolbar.addSeparator();
		toolbar.add(saveAllAction);
		toolbar.add(exportAction);
		toolbar.addSeparator();
		toolbar.add(syncAction);
		toolbar.add(flatPkgButton);
		toolbar.addSeparator();
		toolbar.add(textSearchAction);
		toolbar.add(clsSearchAction);
		toolbar.add(commentSearchAction);
		toolbar.add(goToMainActivityAction);
		toolbar.add(goToApplicationAction);
		toolbar.addSeparator();
		toolbar.add(backAction);
		toolbar.add(forwardAction);
		toolbar.addSeparator();
		toolbar.add(deobfToggleBtn);
		toolbar.add(quarkAction);
		toolbar.add(openDeviceAction);
		toolbar.addSeparator();
		toolbar.add(showLogAction);
		toolbar.addSeparator();
		toolbar.add(prefsAction);
		toolbar.addSeparator();
		toolbar.add(Box.createHorizontalGlue());
		toolbar.add(updateLink);

		mainPanel.add(toolbar, BorderLayout.NORTH);

		nav.add(new HiddenMenuItem(backVariantAction));
		nav.add(new HiddenMenuItem(forwardVariantAction));

		shortcutsController.bind(backVariantAction);
		shortcutsController.bind(forwardVariantAction);

		addLoadListener(loaded -> {
			textSearchAction.setEnabled(loaded);
			clsSearchAction.setEnabled(loaded);
			commentSearchAction.setEnabled(loaded);
			goToMainActivityAction.setEnabled(loaded);
			goToApplicationAction.setEnabled(loaded);
			backAction.setEnabled(loaded);
			backVariantAction.setEnabled(loaded);
			forwardAction.setEnabled(loaded);
			forwardVariantAction.setEnabled(loaded);
			syncAction.setEnabled(loaded);
			saveAllAction.setEnabled(loaded);
			exportAction.setEnabled(loaded);
			saveProjectAsAction.setEnabled(loaded);
			reloadAction.setEnabled(loaded);
			decompileAllAction.setEnabled(loaded);
			deobfAction.setEnabled(loaded);
			quarkAction.setEnabled(loaded);
			resetCacheAction.setEnabled(loaded);
			return false;
		});
	}

	private void initUI() {
		setMinimumSize(new Dimension(200, 150));
		mainPanel = new JPanel(new BorderLayout());
		treeSplitPane = new JSplitPane();
		treeSplitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);
		mainPanel.add(treeSplitPane);

		DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(NLS.str("msg.open_file"));
		treeModel = new DefaultTreeModel(treeRootNode);
		tree = new JTree(treeModel);
		ToolTipManager.sharedInstance().registerComponent(tree);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setFocusable(false);
		tree.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				tree.setFocusable(false);
			}
		});
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					if (!nodeClickAction(getJNodeUnderMouse(e))) {
						// click ignored -> switch to focusable mode
						tree.setFocusable(true);
						tree.requestFocus();
					}
				} else if (SwingUtilities.isRightMouseButton(e)) {
					treeRightClickAction(e);
				}
			}
		});
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					nodeClickAction(tree.getLastSelectedPathComponent());
				}
			}
		});
		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			@Override
			public Component getTreeCellRendererComponent(JTree tree,
					Object value, boolean selected, boolean expanded,
					boolean isLeaf, int row, boolean focused) {
				Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
				if (value instanceof JNode) {
					JNode jNode = (JNode) value;
					NodeLabel.disableHtml(this, jNode.disableHtml());
					setText(jNode.makeStringHtml());
					setIcon(jNode.getIcon());
					setToolTipText(jNode.getTooltip());
				} else {
					setToolTipText(null);
				}
				if (value instanceof JPackage) {
					setEnabled(((JPackage) value).isEnabled());
				}
				return c;
			}
		});
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event) {
				TreePath path = event.getPath();
				Object node = path.getLastPathComponent();
				if (node instanceof JLoadableNode) {
					((JLoadableNode) node).loadNode();
				}
				if (!treeReloading) {
					project.addTreeExpansion(getPathExpansion(event.getPath()));
					update();
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) {
				if (!treeReloading) {
					project.removeTreeExpansion(getPathExpansion(event.getPath()));
					update();
				}
			}
		});

		progressPane = new ProgressPanel(this, true);
		issuesPanel = new IssuesPanel(this);

		JPanel leftPane = new JPanel(new BorderLayout());
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setMinimumSize(new Dimension(100, 150));

		JPanel bottomPane = new JPanel(new BorderLayout());
		bottomPane.add(issuesPanel, BorderLayout.PAGE_START);
		bottomPane.add(progressPane, BorderLayout.PAGE_END);

		leftPane.add(treeScrollPane, BorderLayout.CENTER);
		leftPane.add(bottomPane, BorderLayout.PAGE_END);
		treeSplitPane.setLeftComponent(leftPane);

		tabsController = new TabsController(this);
		tabbedPane = new TabbedPane(this, tabsController);
		tabbedPane.setMinimumSize(new Dimension(150, 150));
		new TabDndController(tabbedPane, settings);

		quickTabsAndCodeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		quickTabsAndCodeSplitPane.setResizeWeight(0.15);
		quickTabsAndCodeSplitPane.setDividerSize(0);
		quickTabsAndCodeSplitPane.setRightComponent(tabbedPane);

		rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		rightSplitPane.setTopComponent(quickTabsAndCodeSplitPane);
		rightSplitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);

		treeSplitPane.setRightComponent(rightSplitPane);

		new DropTarget(this, DnDConstants.ACTION_COPY, new MainDropTarget(this));

		heapUsageBar = new HeapUsageBar();
		mainPanel.add(heapUsageBar, BorderLayout.SOUTH);

		bottomSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		bottomSplitPane.setTopComponent(treeSplitPane);
		bottomSplitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);

		mainPanel.add(bottomSplitPane, BorderLayout.CENTER);
		setContentPane(mainPanel);
		setTitle(DEFAULT_TITLE);
	}

	private static String[] getPathExpansion(TreePath path) {
		List<String> pathList = new ArrayList<>();
		while (path != null) {
			Object node = path.getLastPathComponent();
			String name;
			if (node instanceof JClass) {
				name = ((JClass) node).getCls().getClassNode().getClassInfo().getFullName();
			} else {
				name = node.toString();
			}
			pathList.add(name);
			path = path.getParentPath();
		}
		return pathList.toArray(new String[0]);
	}

	public static void getExpandedPaths(JTree tree, TreePath path, List<TreePath> list) {
		if (tree.isExpanded(path)) {
			list.add(path);

			TreeNode node = (TreeNode) path.getLastPathComponent();
			for (int i = node.getChildCount() - 1; i >= 0; i--) {
				TreeNode n = node.getChildAt(i);
				TreePath child = path.pathByAddingChild(n);
				getExpandedPaths(tree, child, list);
			}
		}
	}

	public void setLocationAndPosition() {
		if (settings.loadWindowPos(this)) {
			return;
		}
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		DisplayMode mode = gd.getDisplayMode();
		AffineTransform trans = gd.getDefaultConfiguration().getDefaultTransform();
		int w = (int) (mode.getWidth() / trans.getScaleX());
		int h = (int) (mode.getHeight() / trans.getScaleY());
		setBounds((int) (w * BORDER_RATIO), (int) (h * BORDER_RATIO),
				(int) (w * WINDOW_RATIO), (int) (h * WINDOW_RATIO));
		setLocationRelativeTo(null);
	}

	private void setEditorTheme(String editorThemePath) {
		try {
			URL themeUrl = getClass().getResource(editorThemePath);
			if (themeUrl != null) {
				try (InputStream is = themeUrl.openStream()) {
					editorTheme = Theme.load(is);
					return;
				}
			}
			Path themePath = Paths.get(editorThemePath);
			if (Files.isRegularFile(themePath)) {
				try (InputStream is = Files.newInputStream(themePath)) {
					editorTheme = Theme.load(is);
					return;
				}
			}
		} catch (Exception e) {
			LOG.error("Failed to load editor theme: {}", editorThemePath, e);
		}
		LOG.warn("Falling back to default editor theme: {}", editorThemePath);
		editorThemePath = EditorTheme.getDefaultTheme().getPath();
		try (InputStream is = getClass().getResourceAsStream(editorThemePath)) {
			editorTheme = Theme.load(is);
			return;
		} catch (Exception e) {
			LOG.error("Failed to load default editor theme: {}", editorThemePath, e);
			editorTheme = new Theme(new RSyntaxTextArea());
		}
	}

	public Theme getEditorTheme() {
		return editorTheme;
	}

	private void openSettings() {
		settingsOpen = true;

		JDialog settingsWindow = new JadxSettingsWindow(MainWindow.this, settings);
		settingsWindow.setVisible(true);
		settingsWindow.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(WindowEvent e) {
				settingsOpen = false;
			}
		});
	}

	public boolean isSettingsOpen() {
		return settingsOpen;
	}

	public void loadSettings() {
		// queue update to not interrupt current UI tasks
		UiUtils.uiRun(this::updateUiSettings);
	}

	private void updateUiSettings() {
		LafManager.updateLaf(settings);

		Font font = settings.getFont();
		Font largerFont = font.deriveFont(font.getSize() + 2.f);

		setFont(largerFont);
		setEditorTheme(settings.getEditorThemePath());
		tree.setFont(largerFont);
		tree.setRowHeight(-1);

		tabbedPane.loadSettings();
		if (logPanel != null) {
			logPanel.loadSettings();
		}
		if (quickTabsTree != null) {
			quickTabsTree.loadSettings();
		}

		shortcutsController.loadSettings();
	}

	private void closeWindow() {
		saveAll();
		if (!ensureProjectIsSaved()) {
			return;
		}
		settings.setTreeWidth(treeSplitPane.getDividerLocation());
		settings.saveWindowPos(this);
		settings.setMainWindowExtendedState(getExtendedState());
		if (debuggerPanel != null) {
			saveSplittersInfo();
		}
		heapUsageBar.reset();
		closeAll();

		FileUtils.deleteTempRootDir();
		dispose();
		System.exit(0);
	}

	private void saveOpenTabs() {
		project.saveOpenTabs(tabsController.getEditorViewStates());
	}

	private void restoreOpenTabs(List<EditorViewState> openTabs) {
		UiUtils.uiThreadGuard();
		if (openTabs.isEmpty()) {
			return;
		}
		for (EditorViewState viewState : openTabs) {
			tabsController.restoreEditorViewState(viewState);
		}
		tabsController.notifyRestoreEditorViewStateDone();
	}

	private void preLoadOpenTabs(List<EditorViewState> openTabs) {
		UiUtils.notUiThreadGuard();
		for (EditorViewState tabState : openTabs) {
			JNode node = tabState.getNode();
			try {
				node.getCodeInfo();
			} catch (Exception e) {
				LOG.warn("Failed to preload code for node: {}", node, e);
			}
		}
	}

	private void saveSplittersInfo() {
		settings.setMainWindowVerticalSplitterLoc(bottomSplitPane.getDividerLocation());
		if (debuggerPanel != null) {
			settings.setDebuggerStackFrameSplitterLoc(debuggerPanel.getLeftSplitterLocation());
			settings.setDebuggerVarTreeSplitterLoc(debuggerPanel.getRightSplitterLocation());
		}
	}

	public void addLoadListener(ILoadListener loadListener) {
		this.loadListeners.add(loadListener);
		// set initial value
		loadListener.update(loaded);
	}

	public void notifyLoadListeners(boolean loaded) {
		this.loaded = loaded;
		loadListeners.removeIf(listener -> listener.update(loaded));
	}

	public void addTreeUpdateListener(Consumer<JRoot> listener) {
		treeUpdateListener.add(listener);
	}

	public JadxWrapper getWrapper() {
		return wrapper;
	}

	public JadxProject getProject() {
		return project;
	}

	public TabbedPane getTabbedPane() {
		return tabbedPane;
	}

	public TabsController getTabsController() {
		return tabsController;
	}

	public JadxSettings getSettings() {
		return settings;
	}

	public CacheObject getCacheObject() {
		return cacheObject;
	}

	public BackgroundExecutor getBackgroundExecutor() {
		return backgroundExecutor;
	}

	public JRoot getTreeRoot() {
		return treeRoot;
	}

	public JDebuggerPanel getDebuggerPanel() {
		initDebuggerPanel();
		return debuggerPanel;
	}

	public ShortcutsController getShortcutsController() {
		return shortcutsController;
	}

	public void showDebuggerPanel() {
		initDebuggerPanel();
	}

	public void destroyDebuggerPanel() {
		saveSplittersInfo();
		if (debuggerPanel != null) {
			debuggerPanel.setVisible(false);
			debuggerPanel = null;
		}
	}

	public void showHeapUsageBar() {
		settings.setShowHeapUsageBar(true);
		heapUsageBar.setVisible(true);
	}

	private void initDebuggerPanel() {
		if (debuggerPanel == null) {
			debuggerPanel = new JDebuggerPanel(this);
			debuggerPanel.loadSettings();
			bottomSplitPane.setBottomComponent(debuggerPanel);
			int loc = settings.getMainWindowVerticalSplitterLoc();
			if (loc == 0) {
				loc = 300;
			}
			bottomSplitPane.setDividerLocation(loc);
		}
	}

	public void showLogViewer(LogOptions logOptions) {
		UiUtils.uiRun(() -> {
			if (settings.isDockLogViewer()) {
				showDockedLog(logOptions);
			} else {
				LogViewerDialog.open(this, logOptions);
			}
		});
	}

	private void showDockedLog(LogOptions logOptions) {
		if (logPanel != null) {
			logPanel.applyLogOptions(logOptions);
			return;
		}
		Runnable undock = () -> {
			hideDockedLog();
			settings.setDockLogViewer(false);
			LogViewerDialog.open(this, logOptions);
		};
		logPanel = new LogPanel(this, logOptions, undock, this::hideDockedLog);
		rightSplitPane.setBottomComponent(logPanel);
	}

	private void hideDockedLog() {
		if (logPanel == null) {
			return;
		}
		logPanel.dispose();
		logPanel = null;
		rightSplitPane.setBottomComponent(null);
	}

	private void setQuickTabsVisibility(boolean visible) {
		if (visible) {
			if (quickTabsTree == null) {
				quickTabsTree = new QuickTabsTree(this);
			}

			quickTabsAndCodeSplitPane.setLeftComponent(quickTabsTree);
			quickTabsAndCodeSplitPane.setDividerSize(5);
		} else {
			quickTabsAndCodeSplitPane.setLeftComponent(null);
			quickTabsAndCodeSplitPane.setDividerSize(0);

			if (quickTabsTree != null) {
				quickTabsTree.dispose();
				quickTabsTree = null;
			}
		}
	}

	public JMenu getPluginsMenu() {
		return pluginsMenu;
	}

	public void resetPluginsMenu() {
		pluginsMenu.removeAll();
		pluginsMenu.add(new ActionHandler(() -> new PluginSettings(this, settings).addPlugin())
				.withNameAndDesc(NLS.str("preferences.plugins.install")));
	}

	public void addToPluginsMenu(Action item) {
		if (pluginsMenu.getMenuComponentCount() == 1) {
			pluginsMenu.addSeparator();
		}
		pluginsMenu.add(item);
	}

	public RenameMappingsGui getRenameMappings() {
		return renameMappings;
	}

	public CacheManager getCacheManager() {
		return cacheManager;
	}

	/**
	 * Events instance if decompiler not yet available
	 */
	private final IJadxEvents fallbackEvents = new JadxEventsImpl();

	public IJadxEvents events() {
		return wrapper.getCurrentDecompiler()
				.map(JadxDecompiler::events)
				.orElse(fallbackEvents);
	}
}
