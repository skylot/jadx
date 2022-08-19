package jadx.gui.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
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
import net.fabricmc.mappingio.format.MappingFormat;

import jadx.api.JadxArgs;
import jadx.api.JavaNode;
import jadx.api.ResourceFile;
import jadx.api.plugins.utils.CommonFileUtils;
import jadx.core.Jadx;
import jadx.core.utils.ListUtils;
import jadx.core.utils.StringUtils;
import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.device.debugger.BreakpointManager;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.DecompileTask;
import jadx.gui.jobs.ExportTask;
import jadx.gui.jobs.ProcessResult;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.plugins.mappings.MappingExporter;
import jadx.gui.plugins.quark.QuarkDialog;
import jadx.gui.settings.JadxProject;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsWindow;
import jadx.gui.treemodel.ApkSignature;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JLoadableNode;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.ui.codearea.AbstractCodeArea;
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.ui.codearea.EditorTheme;
import jadx.gui.ui.codearea.EditorViewState;
import jadx.gui.ui.dialog.ADBDialog;
import jadx.gui.ui.dialog.AboutDialog;
import jadx.gui.ui.dialog.FileDialog;
import jadx.gui.ui.dialog.LogViewerDialog;
import jadx.gui.ui.dialog.RenameDialog;
import jadx.gui.ui.dialog.SearchDialog;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.ui.panel.IssuesPanel;
import jadx.gui.ui.panel.JDebuggerPanel;
import jadx.gui.ui.panel.ProgressPanel;
import jadx.gui.ui.popupmenu.JPackagePopupMenu;
import jadx.gui.ui.treenodes.StartPageNode;
import jadx.gui.ui.treenodes.SummaryNode;
import jadx.gui.update.JadxUpdate;
import jadx.gui.update.JadxUpdate.IUpdateCallback;
import jadx.gui.update.data.Release;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.ILoadListener;
import jadx.gui.utils.Icons;
import jadx.gui.utils.LafManager;
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.SystemInfo;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.fileswatcher.LiveReloadWorker;
import jadx.gui.utils.logs.LogCollector;
import jadx.gui.utils.ui.ActionHandler;

import static io.reactivex.internal.functions.Functions.EMPTY_RUNNABLE;
import static javax.swing.KeyStroke.getKeyStroke;

public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";

	private static final double BORDER_RATIO = 0.15;
	private static final double WINDOW_RATIO = 1 - BORDER_RATIO * 2;
	public static final double SPLIT_PANE_RESIZE_WEIGHT = 0.15;

	private static final ImageIcon ICON_ADD_FILES = UiUtils.openSvgIcon("ui/addFile");
	private static final ImageIcon ICON_SAVE_ALL = UiUtils.openSvgIcon("ui/menu-saveall");
	private static final ImageIcon ICON_RELOAD = UiUtils.openSvgIcon("ui/refresh");
	private static final ImageIcon ICON_EXPORT = UiUtils.openSvgIcon("ui/export");
	private static final ImageIcon ICON_EXIT = UiUtils.openSvgIcon("ui/exit");
	private static final ImageIcon ICON_SYNC = UiUtils.openSvgIcon("ui/pagination");
	private static final ImageIcon ICON_FLAT_PKG = UiUtils.openSvgIcon("ui/moduleGroup");
	private static final ImageIcon ICON_SEARCH = UiUtils.openSvgIcon("ui/find");
	private static final ImageIcon ICON_FIND = UiUtils.openSvgIcon("ui/ejbFinderMethod");
	private static final ImageIcon ICON_COMMENT_SEARCH = UiUtils.openSvgIcon("ui/usagesFinder");
	private static final ImageIcon ICON_BACK = UiUtils.openSvgIcon("ui/left");
	private static final ImageIcon ICON_FORWARD = UiUtils.openSvgIcon("ui/right");
	private static final ImageIcon ICON_QUARK = UiUtils.openSvgIcon("ui/quark");
	private static final ImageIcon ICON_PREF = UiUtils.openSvgIcon("ui/settings");
	private static final ImageIcon ICON_DEOBF = UiUtils.openSvgIcon("ui/helmChartLock");
	private static final ImageIcon ICON_LOG = UiUtils.openSvgIcon("ui/logVerbose");
	private static final ImageIcon ICON_INFO = UiUtils.openSvgIcon("ui/showInfos");
	private static final ImageIcon ICON_DEBUGGER = UiUtils.openSvgIcon("ui/startDebugger");

	private final transient JadxWrapper wrapper;
	private final transient JadxSettings settings;
	private final transient CacheObject cacheObject;
	private final transient BackgroundExecutor backgroundExecutor;

	private transient @NotNull JadxProject project;

	private transient Action newProjectAction;
	private transient Action saveProjectAction;
	private transient JMenu exportMappingsMenu;

	private JPanel mainPanel;
	private JSplitPane splitPane;

	private JTree tree;
	private DefaultTreeModel treeModel;
	private JRoot treeRoot;
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

	private JDebuggerPanel debuggerPanel;
	private JSplitPane verticalSplitter;

	private List<ILoadListener> loadListeners = new ArrayList<>();
	private boolean loaded;

	public MainWindow(JadxSettings settings) {
		this.settings = settings;
		this.cacheObject = new CacheObject();
		this.project = new JadxProject(this);
		this.wrapper = new JadxWrapper(this);
		this.liveReloadWorker = new LiveReloadWorker(this);

		resetCache();
		FontUtils.registerBundledFonts();
		initUI();
		this.backgroundExecutor = new BackgroundExecutor(settings, progressPane);
		initMenuAndToolbar();
		registerMouseNavigationButtons();
		UiUtils.setWindowIcons(this);
		loadSettings();

		update();
		checkForUpdate();
	}

	public void init() {
		pack();
		setLocationAndPosition();
		splitPane.setDividerLocation(settings.getTreeWidth());
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
		JadxUpdate.check(new IUpdateCallback() {
			@Override
			public void onUpdate(Release r) {
				SwingUtilities.invokeLater(() -> {
					updateLink.setText(NLS.str("menu.update_label", r.getName()));
					updateLink.setVisible(true);
				});
			}
		});
	}

	public void openFileDialog() {
		showOpenDialog(FileDialog.OpenMode.OPEN);
	}

	public void openProjectDialog() {
		showOpenDialog(FileDialog.OpenMode.OPEN_PROJECT);
	}

	private void showOpenDialog(FileDialog.OpenMode mode) {
		saveAll();
		if (!ensureProjectIsSaved()) {
			return;
		}
		FileDialog fileDialog = new FileDialog(this, mode);
		List<Path> openPaths = fileDialog.show();
		if (!openPaths.isEmpty()) {
			settings.setLastOpenFilePath(fileDialog.getCurrentDir());
			open(openPaths);
		}
	}

	public void addFiles() {
		FileDialog fileDialog = new FileDialog(this, FileDialog.OpenMode.ADD);
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
		exportMappingsMenu.setEnabled(false);
		updateProject(new JadxProject(this));
	}

	private void saveProject() {
		if (!project.isSaveFileSelected()) {
			saveProjectAs();
		} else {
			project.save();
			update();
		}
	}

	private void saveProjectAs() {
		FileDialog fileDialog = new FileDialog(this, FileDialog.OpenMode.SAVE_PROJECT);
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

	private void exportMappings(MappingFormat mappingFormat) {
		FileDialog fileDialog = new FileDialog(this, FileDialog.OpenMode.CUSTOM_SAVE);
		fileDialog.setTitle(NLS.str("file.export_mappings_as"));
		Path workingDir = project.getWorkingDir();
		Path baseDir = workingDir != null ? workingDir : settings.getLastSaveFilePath();
		if (mappingFormat.hasSingleFile()) {
			fileDialog.setSelectedFile(baseDir.resolve("mappings." + mappingFormat.fileExt));
			fileDialog.setFileExtList(Collections.singletonList(mappingFormat.fileExt));
			fileDialog.setSelectionMode(JFileChooser.FILES_ONLY);
		} else {
			fileDialog.setCurrentDir(baseDir);
			fileDialog.setSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		}
		List<Path> paths = fileDialog.show();
		if (paths.size() != 1) {
			return;
		}
		Path savePath = paths.get(0);
		LOG.info("Export mappings to: {}", savePath.toAbsolutePath());
		backgroundExecutor.execute(NLS.str("progress.export_mappings"),
				() -> new MappingExporter(wrapper.getDecompiler().getRoot())
						.exportMappings(savePath, project.getCodeData(), mappingFormat),
				s -> update());
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
		String fileExtension = CommonFileUtils.getFileExtension(singleFile.getFileName().toString());
		if (fileExtension != null && fileExtension.equalsIgnoreCase(JadxProject.PROJECT_EXTENSION)) {
			openProject(singleFile, onFinish);
			return true;
		}
		// check if project file already saved with default name
		Path projectPath = getProjectPathForFile(singleFile);
		if (Files.exists(projectPath)) {
			LOG.info("Loading project {}", projectPath);
			openProject(projectPath, onFinish);
			return true;
		}
		return false;
	}

	private static Path getProjectPathForFile(Path loadedFile) {
		String fileName = loadedFile.getFileName() + "." + JadxProject.PROJECT_EXTENSION;
		return loadedFile.resolveSibling(fileName);
	}

	public synchronized void reopen() {
		saveAll();
		closeAll();
		loadFiles(EMPTY_RUNNABLE);
	}

	private void openProject(Path path, Runnable onFinish) {
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
		exportMappingsMenu.setEnabled(false);
		if (project.getFilePaths().isEmpty()) {
			return;
		}
		backgroundExecutor.execute(NLS.str("progress.load"),
				wrapper::open,
				status -> {
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
					exportMappingsMenu.setEnabled(true);
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
		tabbedPane.closeAllTabs();
		UiUtils.resetClipboardOwner();
		System.gc();
	}

	private void checkLoadedStatus() {
		if (!wrapper.getClasses().isEmpty()) {
			return;
		}
		int errors = LogCollector.getInstance().getErrors();
		if (errors > 0) {
			int result = JOptionPane.showConfirmDialog(this,
					NLS.str("message.load_errors", errors),
					NLS.str("message.errorTitle"),
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.ERROR_MESSAGE);
			if (result == JOptionPane.OK_OPTION) {
				LogViewerDialog.openWithLevel(this, Level.ERROR);
			}
		} else {
			UiUtils.showMessageBox(this, NLS.str("message.no_classes"));
		}
	}

	private void onOpen() {
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		initTree();
		update();
		updateLiveReload(project.isEnableLiveReload());
		BreakpointManager.init(project.getFilePaths().get(0).toAbsolutePath().getParent());

		backgroundExecutor.execute(NLS.str("progress.load"),
				this::restoreOpenTabs,
				status -> {
					runInitialBackgroundJobs();
					notifyLoadListeners(true);
				});
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
			int res = JOptionPane.showConfirmDialog(
					this,
					NLS.str("confirm.not_saved_message"),
					NLS.str("confirm.not_saved_title"),
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (res == JOptionPane.CANCEL_OPTION) {
				return false;
			}
			if (res == JOptionPane.YES_OPTION) {
				saveProject();
			}
		}
		return true;
	}

	public void updateProject(@NotNull JadxProject jadxProject) {
		this.project = jadxProject;
		update();
	}

	private void update() {
		newProjectAction.setEnabled(!project.isInitial());
		saveProjectAction.setEnabled(!project.isSaved());

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
					waitDecompileTask();
				}
			}, 1000);
		}
	}

	private static final Object DECOMPILER_TASK_SYNC = new Object();

	public void waitDecompileTask() {
		synchronized (DECOMPILER_TASK_SYNC) {
			try {
				DecompileTask decompileTask = new DecompileTask(wrapper);
				backgroundExecutor.executeAndWait(decompileTask);
				backgroundExecutor.execute(decompileTask.getTitle(), wrapper::unloadClasses).get();
				processDecompilationResults(decompileTask.getResult());
				System.gc();
			} catch (Exception e) {
				LOG.error("Decompile task execution failed", e);
			}
		}
	}

	private void processDecompilationResults(ProcessResult decompile) {
		int skippedCls = decompile.getSkipped();
		if (skippedCls == 0) {
			return;
		}
		TaskStatus status = decompile.getStatus();
		LOG.warn("Decompile and indexing of some classes skipped: {}, status: {}", skippedCls, status);
		switch (status) {
			case CANCEL_BY_USER: {
				String reason = NLS.str("message.userCancelTask");
				String message = NLS.str("message.indexIncomplete", reason, skippedCls);
				JOptionPane.showMessageDialog(this, message);
				break;
			}
			case CANCEL_BY_TIMEOUT: {
				String reason = NLS.str("message.taskTimeout", decompile.getTimeLimit());
				String message = NLS.str("message.indexIncomplete", reason, skippedCls);
				JOptionPane.showMessageDialog(this, message);
				break;
			}
			case CANCEL_BY_MEMORY: {
				showHeapUsageBar();
				JOptionPane.showMessageDialog(this, NLS.str("message.indexingClassesSkipped", skippedCls));
				break;
			}
		}
	}

	public void cancelBackgroundJobs() {
		backgroundExecutor.cancelAll();
	}

	private void saveAll(boolean export) {
		FileDialog fileDialog = new FileDialog(this, FileDialog.OpenMode.EXPORT);
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

		treeModel.reload();
		List<String[]> treeExpansions = project.getTreeExpansions();
		if (!treeExpansions.isEmpty()) {
			expand(treeRoot, treeExpansions);
		} else {
			tree.expandRow(1);
		}

		treeReloading = false;
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
		JNode obj = getJNodeUnderMouse(e);
		if (obj instanceof JPackage) {
			JPackagePopupMenu menu = new JPackagePopupMenu(this, (JPackage) obj);
			menu.show(e.getComponent(), e.getX(), e.getY());
		} else if (obj instanceof JClass || obj instanceof JField || obj instanceof JMethod) {
			JMenuItem jmi = new JMenuItem(NLS.str("popup.rename"));
			jmi.addActionListener(action -> RenameDialog.rename(this, obj));
			JPopupMenu menu = new JPopupMenu();
			menu.add(jmi);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Nullable
	private JNode getJNodeUnderMouse(MouseEvent mouseEvent) {
		TreePath path = tree.getClosestPathForLocation(mouseEvent.getX(), mouseEvent.getY());
		if (path == null) {
			return null;
		}
		// allow 'closest' path only at the right of the item row
		Rectangle pathBounds = tree.getPathBounds(path);
		if (pathBounds != null) {
			int y = mouseEvent.getY();
			if (y < pathBounds.y || y > (pathBounds.y + pathBounds.height)) {
				return null;
			}
			if (mouseEvent.getX() < pathBounds.x) {
				// exclude expand/collapse events
				return null;
			}
		}
		Object obj = path.getLastPathComponent();
		if (obj instanceof JNode) {
			tree.setSelectionPath(path);
			return (JNode) obj;
		}
		return null;
	}

	public void syncWithEditor() {
		ContentPanel selectedContentPanel = tabbedPane.getSelectedCodePanel();
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

	private void initMenuAndToolbar() {
		ActionHandler openAction = new ActionHandler(this::openFileDialog);
		openAction.setNameAndDesc(NLS.str("file.open_action"));
		openAction.setIcon(Icons.OPEN);
		openAction.setKeyBinding(getKeyStroke(KeyEvent.VK_O, UiUtils.ctrlButton()));

		ActionHandler openProject = new ActionHandler(this::openProjectDialog);
		openProject.setNameAndDesc(NLS.str("file.open_project"));
		openProject.setIcon(Icons.OPEN_PROJECT);
		openProject.setKeyBinding(getKeyStroke(KeyEvent.VK_O, InputEvent.SHIFT_DOWN_MASK | UiUtils.ctrlButton()));

		Action addFilesAction = new AbstractAction(NLS.str("file.add_files_action"), ICON_ADD_FILES) {
			@Override
			public void actionPerformed(ActionEvent e) {
				addFiles();
			}
		};
		addFilesAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.add_files_action"));

		newProjectAction = new AbstractAction(NLS.str("file.new_project"), Icons.NEW_PROJECT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				newProject();
			}
		};
		newProjectAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.new_project"));

		saveProjectAction = new AbstractAction(NLS.str("file.save_project")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveProject();
			}
		};
		saveProjectAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.save_project"));

		Action saveProjectAsAction = new AbstractAction(NLS.str("file.save_project_as")) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveProjectAs();
			}
		};
		saveProjectAsAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.save_project_as"));

		ActionHandler reload = new ActionHandler(ev -> UiUtils.uiRun(this::reopen));
		reload.setNameAndDesc(NLS.str("file.reload"));
		reload.setIcon(ICON_RELOAD);
		reload.setKeyBinding(getKeyStroke(KeyEvent.VK_F5, 0));

		ActionHandler liveReload = new ActionHandler(ev -> updateLiveReload(!project.isEnableLiveReload()));
		liveReload.setName(NLS.str("file.live_reload"));
		liveReload.setShortDescription(NLS.str("file.live_reload_desc"));
		liveReload.setKeyBinding(getKeyStroke(KeyEvent.VK_F5, InputEvent.SHIFT_DOWN_MASK));

		liveReloadMenuItem = new JCheckBoxMenuItem(liveReload);
		liveReloadMenuItem.setState(project.isEnableLiveReload());

		Action exportMappingsAsTiny2 = new AbstractAction("Tiny v2 file") {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportMappings(MappingFormat.TINY_2);
			}
		};
		exportMappingsAsTiny2.putValue(Action.SHORT_DESCRIPTION, "Tiny v2 file");

		Action exportMappingsAsEnigma = new AbstractAction("Enigma file") {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportMappings(MappingFormat.ENIGMA);
			}
		};
		exportMappingsAsEnigma.putValue(Action.SHORT_DESCRIPTION, "Enigma file");

		Action exportMappingsAsEnigmaDir = new AbstractAction("Enigma directory") {
			@Override
			public void actionPerformed(ActionEvent e) {
				exportMappings(MappingFormat.ENIGMA_DIR);
			}
		};
		exportMappingsAsEnigmaDir.putValue(Action.SHORT_DESCRIPTION, "Enigma directory");

		exportMappingsMenu = new JMenu(NLS.str("file.export_mappings_as"));
		exportMappingsMenu.add(exportMappingsAsTiny2);
		exportMappingsMenu.add(exportMappingsAsEnigma);
		exportMappingsMenu.add(exportMappingsAsEnigmaDir);
		exportMappingsMenu.setEnabled(false);

		Action saveAllAction = new AbstractAction(NLS.str("file.save_all"), ICON_SAVE_ALL) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAll(false);
			}
		};
		saveAllAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.save_all"));
		saveAllAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_S, UiUtils.ctrlButton()));

		Action exportAction = new AbstractAction(NLS.str("file.export_gradle"), ICON_EXPORT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAll(true);
			}
		};
		exportAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.export_gradle"));
		exportAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_E, UiUtils.ctrlButton()));

		JMenu recentProjects = new JMenu(NLS.str("menu.recent_projects"));
		recentProjects.addMenuListener(new RecentProjectsMenuListener(recentProjects));

		Action prefsAction = new AbstractAction(NLS.str("menu.preferences"), ICON_PREF) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new JadxSettingsWindow(MainWindow.this, settings).setVisible(true);
			}
		};
		prefsAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.preferences"));
		prefsAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_P,
				UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK));

		Action exitAction = new AbstractAction(NLS.str("file.exit"), ICON_EXIT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				closeWindow();
			}
		};

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

		Action syncAction = new AbstractAction(NLS.str("menu.sync"), ICON_SYNC) {
			@Override
			public void actionPerformed(ActionEvent e) {
				syncWithEditor();
			}
		};
		syncAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.sync"));
		syncAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_T, UiUtils.ctrlButton()));

		Action textSearchAction = new AbstractAction(NLS.str("menu.text_search"), ICON_SEARCH) {
			@Override
			public void actionPerformed(ActionEvent e) {
				ContentPanel panel = tabbedPane.getSelectedCodePanel();
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
		};
		textSearchAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.text_search"));
		textSearchAction.putValue(Action.ACCELERATOR_KEY,
				getKeyStroke(KeyEvent.VK_F, UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK));

		Action clsSearchAction = new AbstractAction(NLS.str("menu.class_search"), ICON_FIND) {
			@Override
			public void actionPerformed(ActionEvent e) {
				SearchDialog.search(MainWindow.this, SearchDialog.SearchPreset.CLASS);
			}
		};
		clsSearchAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.class_search"));
		clsSearchAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_N, UiUtils.ctrlButton()));

		Action commentSearchAction = new AbstractAction(NLS.str("menu.comment_search"), ICON_COMMENT_SEARCH) {
			@Override
			public void actionPerformed(ActionEvent e) {
				SearchDialog.search(MainWindow.this, SearchDialog.SearchPreset.COMMENT);
			}
		};
		commentSearchAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.comment_search"));
		commentSearchAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_SEMICOLON,
				UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK));

		Action deobfAction = new AbstractAction(NLS.str("menu.deobfuscation"), ICON_DEOBF) {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleDeobfuscation();
			}
		};
		deobfAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("preferences.deobfuscation"));
		deobfAction.putValue(Action.ACCELERATOR_KEY,
				getKeyStroke(KeyEvent.VK_D, UiUtils.ctrlButton() | KeyEvent.ALT_DOWN_MASK));

		deobfToggleBtn = new JToggleButton(deobfAction);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		deobfToggleBtn.setText("");

		deobfMenuItem = new JCheckBoxMenuItem(deobfAction);
		deobfMenuItem.setState(settings.isDeobfuscationOn());

		Action logAction = new AbstractAction(NLS.str("menu.log"), ICON_LOG) {
			@Override
			public void actionPerformed(ActionEvent e) {
				LogViewerDialog.open(MainWindow.this);
			}
		};
		logAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.log"));
		logAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_L,
				UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK));

		Action aboutAction = new AbstractAction(NLS.str("menu.about"), ICON_INFO) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AboutDialog().setVisible(true);
			}
		};

		Action backAction = new AbstractAction(NLS.str("nav.back"), ICON_BACK) {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navBack();
			}
		};
		backAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("nav.back"));
		backAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_ESCAPE, 0));

		Action forwardAction = new AbstractAction(NLS.str("nav.forward"), ICON_FORWARD) {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navForward();
			}
		};
		forwardAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("nav.forward"));
		forwardAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK, SystemInfo.IS_MAC));

		Action quarkAction = new AbstractAction("Quark Engine", ICON_QUARK) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new QuarkDialog(MainWindow.this).setVisible(true);
			}
		};
		quarkAction.putValue(Action.SHORT_DESCRIPTION, "Quark Engine");

		Action openDeviceAction = new AbstractAction(NLS.str("debugger.process_selector"), ICON_DEBUGGER) {
			@Override
			public void actionPerformed(ActionEvent e) {
				ADBDialog dialog = new ADBDialog(MainWindow.this);
				dialog.setVisible(true);
			}
		};
		openDeviceAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("debugger.process_selector"));

		JMenu file = new JMenu(NLS.str("menu.file"));
		file.setMnemonic(KeyEvent.VK_F);
		file.add(openAction);
		file.add(openProject);
		file.add(addFilesAction);
		file.addSeparator();
		file.add(newProjectAction);
		file.add(saveProjectAction);
		file.add(saveProjectAsAction);
		file.addSeparator();
		file.add(reload);
		file.add(liveReloadMenuItem);
		file.addSeparator();
		file.add(exportMappingsMenu);
		file.addSeparator();
		file.add(saveAllAction);
		file.add(exportAction);
		file.addSeparator();
		file.add(recentProjects);
		file.addSeparator();
		file.add(prefsAction);
		file.addSeparator();
		file.add(exitAction);

		JMenu view = new JMenu(NLS.str("menu.view"));
		view.setMnemonic(KeyEvent.VK_V);
		view.add(flatPkgMenuItem);
		view.add(syncAction);
		view.add(heapUsageBarMenuItem);
		view.add(alwaysSelectOpened);

		JMenu nav = new JMenu(NLS.str("menu.navigation"));
		nav.setMnemonic(KeyEvent.VK_N);
		nav.add(textSearchAction);
		nav.add(clsSearchAction);
		nav.add(commentSearchAction);
		nav.addSeparator();
		nav.add(backAction);
		nav.add(forwardAction);

		JMenu tools = new JMenu(NLS.str("menu.tools"));
		tools.setMnemonic(KeyEvent.VK_T);
		tools.add(deobfMenuItem);
		tools.add(quarkAction);
		tools.add(openDeviceAction);

		JMenu help = new JMenu(NLS.str("menu.help"));
		help.setMnemonic(KeyEvent.VK_H);
		help.add(logAction);
		if (Jadx.isDevVersion()) {
			help.add(new AbstractAction("Show sample error report") {
				@Override
				public void actionPerformed(ActionEvent e) {
					ExceptionDialog.throwTestException();
				}
			});
		}
		help.add(aboutAction);

		JMenuBar menuBar = new JMenuBar();
		menuBar.add(file);
		menuBar.add(view);
		menuBar.add(nav);
		menuBar.add(tools);
		menuBar.add(help);
		setJMenuBar(menuBar);

		flatPkgButton = new JToggleButton(ICON_FLAT_PKG);
		flatPkgButton.setSelected(isFlattenPackage);
		ActionListener flatPkgAction = e -> toggleFlattenPackage();
		flatPkgMenuItem.addActionListener(flatPkgAction);
		flatPkgButton.addActionListener(flatPkgAction);
		flatPkgButton.setToolTipText(NLS.str("menu.flatten"));

		updateLink = new Link("", JadxUpdate.JADX_RELEASES_URL);
		updateLink.setVisible(false);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		toolbar.add(openAction);
		toolbar.add(addFilesAction);
		toolbar.addSeparator();
		toolbar.add(reload);
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
		toolbar.addSeparator();
		toolbar.add(backAction);
		toolbar.add(forwardAction);
		toolbar.addSeparator();
		toolbar.add(deobfToggleBtn);
		toolbar.add(quarkAction);
		toolbar.add(openDeviceAction);
		toolbar.addSeparator();
		toolbar.add(logAction);
		toolbar.addSeparator();
		toolbar.add(prefsAction);
		toolbar.addSeparator();
		toolbar.add(Box.createHorizontalGlue());
		toolbar.add(updateLink);

		mainPanel.add(toolbar, BorderLayout.NORTH);

		addLoadListener(loaded -> {
			textSearchAction.setEnabled(loaded);
			clsSearchAction.setEnabled(loaded);
			commentSearchAction.setEnabled(loaded);
			backAction.setEnabled(loaded);
			forwardAction.setEnabled(loaded);
			syncAction.setEnabled(loaded);
			saveAllAction.setEnabled(loaded);
			exportAction.setEnabled(loaded);
			saveProjectAsAction.setEnabled(loaded);
			reload.setEnabled(loaded);
			deobfAction.setEnabled(loaded);
			quarkAction.setEnabled(loaded);
			return false;
		});
	}

	private void initUI() {
		setMinimumSize(new Dimension(200, 150));
		mainPanel = new JPanel(new BorderLayout());
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);
		mainPanel.add(splitPane);

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
		IssuesPanel issuesPanel = new IssuesPanel(this);

		JPanel leftPane = new JPanel(new BorderLayout());
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setMinimumSize(new Dimension(100, 150));

		JPanel bottomPane = new JPanel(new BorderLayout());
		bottomPane.add(issuesPanel, BorderLayout.PAGE_START);
		bottomPane.add(progressPane, BorderLayout.PAGE_END);

		leftPane.add(treeScrollPane, BorderLayout.CENTER);
		leftPane.add(bottomPane, BorderLayout.PAGE_END);
		splitPane.setLeftComponent(leftPane);

		tabbedPane = new TabbedPane(this);
		tabbedPane.setMinimumSize(new Dimension(150, 150));
		splitPane.setRightComponent(tabbedPane);

		new DropTarget(this, DnDConstants.ACTION_COPY, new MainDropTarget(this));

		heapUsageBar = new HeapUsageBar();
		mainPanel.add(heapUsageBar, BorderLayout.SOUTH);

		verticalSplitter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		verticalSplitter.setTopComponent(splitPane);
		verticalSplitter.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);

		mainPanel.add(verticalSplitter, BorderLayout.CENTER);
		setContentPane(mainPanel);
		setTitle(DEFAULT_TITLE);
	}

	private void registerMouseNavigationButtons() {
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		toolkit.addAWTEventListener(event -> {
			if (event instanceof MouseEvent) {
				MouseEvent mouseEvent = (MouseEvent) event;
				if (mouseEvent.getID() == MouseEvent.MOUSE_PRESSED) {
					int rawButton = mouseEvent.getButton();
					if (rawButton <= 3) {
						return;
					}
					int button = remapMouseButton(rawButton);
					switch (button) {
						case 4:
							tabbedPane.navBack();
							break;
						case 5:
							tabbedPane.navForward();
							break;
					}
				}
			}
		}, AWTEvent.MOUSE_EVENT_MASK);
	}

	private static int remapMouseButton(int rawButton) {
		if (SystemInfo.IS_LINUX) {
			if (rawButton == 6) {
				return 4;
			}
			if (rawButton == 7) {
				return 5;
			}
		}
		return rawButton;
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
		int w = mode.getWidth();
		int h = mode.getHeight();
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

	public void loadSettings() {
		LafManager.updateLaf(settings);

		Font font = settings.getFont();
		Font largerFont = font.deriveFont(font.getSize() + 2.f);

		setFont(largerFont);
		setEditorTheme(settings.getEditorThemePath());
		tree.setFont(largerFont);
		tree.setRowHeight(-1);

		tabbedPane.loadSettings();
	}

	private void closeWindow() {
		saveAll();
		if (!ensureProjectIsSaved()) {
			return;
		}
		settings.setTreeWidth(splitPane.getDividerLocation());
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
		project.saveOpenTabs(tabbedPane.getEditorViewStates(), tabbedPane.getSelectedIndex());
	}

	private void restoreOpenTabs() {
		List<EditorViewState> openTabs = project.getOpenTabs(this);
		if (openTabs.isEmpty()) {
			return;
		}
		for (EditorViewState viewState : openTabs) {
			tabbedPane.restoreEditorViewState(viewState);
		}
		try {
			tabbedPane.setSelectedIndex(project.getActiveTab());
		} catch (Exception e) {
			LOG.warn("Failed to restore active tab", e);
		}
	}

	private void saveSplittersInfo() {
		settings.setMainWindowVerticalSplitterLoc(verticalSplitter.getDividerLocation());
		settings.setDebuggerStackFrameSplitterLoc(debuggerPanel.getLeftSplitterLocation());
		settings.setDebuggerVarTreeSplitterLoc(debuggerPanel.getRightSplitterLocation());
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

	public JadxWrapper getWrapper() {
		return wrapper;
	}

	public JadxProject getProject() {
		return project;
	}

	public TabbedPane getTabbedPane() {
		return tabbedPane;
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

	public void showDebuggerPanel() {
		initDebuggerPanel();
	}

	public void destroyDebuggerPanel() {
		saveSplittersInfo();
		debuggerPanel.setVisible(false);
		debuggerPanel = null;
	}

	public void showHeapUsageBar() {
		settings.setShowHeapUsageBar(true);
		heapUsageBar.setVisible(true);
	}

	private void initDebuggerPanel() {
		if (debuggerPanel == null) {
			debuggerPanel = new JDebuggerPanel(this);
			debuggerPanel.loadSettings();
			verticalSplitter.setBottomComponent(debuggerPanel);
			int loc = settings.getMainWindowVerticalSplitterLoc();
			if (loc == 0) {
				loc = 300;
			}
			verticalSplitter.setDividerLocation(loc);
		}
	}

	private class RecentProjectsMenuListener implements MenuListener {
		private final JMenu menu;

		public RecentProjectsMenuListener(JMenu menu) {
			this.menu = menu;
		}

		@Override
		public void menuSelected(MenuEvent menuEvent) {
			Set<Path> current = new HashSet<>(project.getFilePaths());
			List<JMenuItem> items = settings.getRecentProjects()
					.stream()
					.filter(path -> !current.contains(path))
					.map(path -> {
						JMenuItem menuItem = new JMenuItem(path.toAbsolutePath().toString());
						menuItem.addActionListener(e -> open(Collections.singletonList(path)));
						return menuItem;
					}).collect(Collectors.toList());

			menu.removeAll();
			if (items.isEmpty()) {
				menu.add(new JMenuItem(NLS.str("menu.no_recent_projects")));
			} else {
				items.forEach(menu::add);
			}
		}

		@Override
		public void menuDeselected(MenuEvent e) {
		}

		@Override
		public void menuCanceled(MenuEvent e) {
		}
	}
}
