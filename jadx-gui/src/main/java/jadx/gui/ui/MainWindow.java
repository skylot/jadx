package jadx.gui.ui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
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
import javax.swing.WindowConstants;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.fife.ui.rsyntaxtextarea.Theme;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.api.JavaClass;
import jadx.api.JavaNode;
import jadx.api.ResourceFile;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.files.FileUtils;
import jadx.gui.JadxWrapper;
import jadx.gui.device.debugger.BreakpointManager;
import jadx.gui.jobs.BackgroundExecutor;
import jadx.gui.jobs.DecompileTask;
import jadx.gui.jobs.ExportTask;
import jadx.gui.jobs.IndexService;
import jadx.gui.jobs.TaskStatus;
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
import jadx.gui.ui.codearea.AbstractCodeContentPanel;
import jadx.gui.update.JadxUpdate;
import jadx.gui.update.JadxUpdate.IUpdateCallback;
import jadx.gui.update.data.Release;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.CodeUsageInfo;
import jadx.gui.utils.FontUtils;
import jadx.gui.utils.JumpPosition;
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.SystemInfo;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.search.CommentsIndex;
import jadx.gui.utils.search.TextSearchIndex;

import static io.reactivex.internal.functions.Functions.EMPTY_RUNNABLE;
import static jadx.gui.utils.FileUtils.fileNamesToPaths;
import static jadx.gui.utils.FileUtils.toPaths;
import static javax.swing.KeyStroke.getKeyStroke;

public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";

	private static final double BORDER_RATIO = 0.15;
	private static final double WINDOW_RATIO = 1 - BORDER_RATIO * 2;
	public static final double SPLIT_PANE_RESIZE_WEIGHT = 0.15;

	private static final ImageIcon ICON_OPEN = UiUtils.openIcon("folder");
	private static final ImageIcon ICON_ADD_FILES = UiUtils.openIcon("folder_add");
	private static final ImageIcon ICON_SAVE_ALL = UiUtils.openIcon("disk_multiple");
	private static final ImageIcon ICON_EXPORT = UiUtils.openIcon("database_save");
	private static final ImageIcon ICON_CLOSE = UiUtils.openIcon("cross");
	private static final ImageIcon ICON_SYNC = UiUtils.openIcon("sync");
	private static final ImageIcon ICON_FLAT_PKG = UiUtils.openIcon("empty_logical_package_obj");
	private static final ImageIcon ICON_SEARCH = UiUtils.openIcon("wand");
	private static final ImageIcon ICON_FIND = UiUtils.openIcon("magnifier");
	private static final ImageIcon ICON_COMMENT_SEARCH = UiUtils.openIcon("table_edit");
	private static final ImageIcon ICON_BACK = UiUtils.openIcon("icon_back");
	private static final ImageIcon ICON_FORWARD = UiUtils.openIcon("icon_forward");
	private static final ImageIcon ICON_QUARK = UiUtils.openIcon("icon_quark");
	private static final ImageIcon ICON_PREF = UiUtils.openIcon("wrench");
	private static final ImageIcon ICON_DEOBF = UiUtils.openIcon("lock_edit");
	private static final ImageIcon ICON_LOG = UiUtils.openIcon("report");
	private static final ImageIcon ICON_JADX = UiUtils.openIcon("jadx-logo");
	private static final ImageIcon ICON_DEBUGGER = UiUtils.openIcon("debugger");

	private final transient JadxWrapper wrapper;
	private final transient JadxSettings settings;
	private final transient CacheObject cacheObject;
	private final transient BackgroundExecutor backgroundExecutor;
	private transient JadxProject project;
	private transient Action newProjectAction;
	private transient Action saveProjectAction;

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

	private transient Link updateLink;
	private transient ProgressPanel progressPane;
	private transient Theme editorTheme;

	private JDebuggerPanel debuggerPanel;
	private JSplitPane verticalSplitter;

	public MainWindow(JadxSettings settings) {
		this.wrapper = new JadxWrapper(settings);
		this.settings = settings;
		this.cacheObject = new CacheObject();

		resetCache();
		FontUtils.registerBundledFonts();
		initUI();
		initMenuAndToolbar();
		registerMouseNavigationButtons();
		UiUtils.setWindowIcons(this);
		loadSettings();

		this.backgroundExecutor = new BackgroundExecutor(this);

		checkForUpdate();
		newProject();
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
			openFileOrProject();
		} else {
			open(fileNamesToPaths(settings.getFiles()), this::handleSelectClassOption);
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
			JNode node = cacheObject.getNodeCache().makeFrom(javaNode);
			tabbedPane.codeJump(new JumpPosition(node.getRootClass(), node.getLine(), JumpPosition.getDefPos(node)));
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

	public void openFileOrProject() {
		String title = NLS.str("file.open_title");
		JFileChooser fileChooser = buildFileChooser(false, title);
		int ret = fileChooser.showDialog(this, title);
		if (ret == JFileChooser.APPROVE_OPTION) {
			settings.setLastOpenFilePath(fileChooser.getCurrentDirectory().toPath());
			open(toPaths(fileChooser.getSelectedFiles()));
		}
	}

	public void addFiles() {
		String title = NLS.str("file.add_files_action");
		JFileChooser fileChooser = buildFileChooser(true, title);
		int ret = fileChooser.showDialog(this, title);
		if (ret == JFileChooser.APPROVE_OPTION) {
			List<Path> paths = new ArrayList<>(wrapper.getOpenPaths());
			paths.addAll(toPaths(fileChooser.getSelectedFiles()));
			open(paths);
		}
	}

	private JFileChooser buildFileChooser(boolean addFiles, String toolTipText) {
		String[] exts;
		if (addFiles) {
			exts = new String[] { "apk", "dex", "jar", "class", "smali", "zip", "aar", "arsc" };
		} else {
			exts = new String[] { JadxProject.PROJECT_EXTENSION, "apk", "dex", "jar", "class", "smali", "zip", "aar", "arsc", "aab" };
		}
		String description = "Supported files: (" + Utils.arrayToStr(exts) + ')';

		JFileChooser fileChooser = new JFileChooser() {
			@Override
			protected JDialog createDialog(Component parent) throws HeadlessException {
				JDialog dialog = super.createDialog(parent);
				dialog.setLocationRelativeTo(null);
				settings.loadWindowPos(dialog);
				dialog.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosed(WindowEvent e) {
						settings.saveWindowPos(dialog);
						super.windowClosed(e);
					}
				});
				return dialog;
			}
		};
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, exts));
		fileChooser.setMultiSelectionEnabled(true);
		fileChooser.setToolTipText(toolTipText);
		Path currentDirectory = settings.getLastOpenFilePath();
		if (currentDirectory != null) {
			fileChooser.setCurrentDirectory(currentDirectory.toFile());
		}
		return fileChooser;
	}

	private void newProject() {
		if (!ensureProjectIsSaved()) {
			return;
		}
		cancelBackgroundJobs();
		clearTree();
		wrapper.close();
		updateProject(new JadxProject());
	}

	private void saveProject() {
		if (project.getProjectPath() == null) {
			saveProjectAs();
		} else {
			project.save();
			update();
		}
	}

	private void saveProjectAs() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(true);
		String[] exts = { JadxProject.PROJECT_EXTENSION };
		String description = "supported files: " + Arrays.toString(exts).replace('[', '(').replace(']', ')');
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, exts));
		fileChooser.setToolTipText(NLS.str("file.save_project"));
		Path currentDirectory = settings.getLastSaveProjectPath();
		if (currentDirectory != null) {
			fileChooser.setCurrentDirectory(currentDirectory.toFile());
		}
		int ret = fileChooser.showSaveDialog(mainPanel);
		if (ret == JFileChooser.APPROVE_OPTION) {
			settings.setLastSaveProjectPath(fileChooser.getCurrentDirectory().toPath());

			Path path = fileChooser.getSelectedFile().toPath();
			if (!path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JadxProject.PROJECT_EXTENSION)) {
				path = path.resolveSibling(path.getFileName() + "." + JadxProject.PROJECT_EXTENSION);
			}

			if (Files.exists(path)) {
				int res = JOptionPane.showConfirmDialog(
						this,
						NLS.str("confirm.save_as_message", path.getFileName()),
						NLS.str("confirm.save_as_title"),
						JOptionPane.YES_NO_OPTION);
				if (res == JOptionPane.NO_OPTION) {
					return;
				}
			}
			project.saveAs(path);
			settings.addRecentProject(path);
			update();
		}
	}

	void open(List<Path> paths) {
		open(paths, EMPTY_RUNNABLE);
	}

	void open(List<Path> paths, Runnable onFinish) {
		if (paths.size() == 1) {
			Path singleFile = paths.get(0);
			if (singleFile.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(JadxProject.PROJECT_EXTENSION)) {
				openProject(singleFile);
				onFinish.run();
				return;
			}
		}
		project.setFilePath(paths);
		clearTree();
		BreakpointManager.saveAndExit();
		if (paths.isEmpty()) {
			return;
		}
		backgroundExecutor.execute(NLS.str("progress.load"),
				() -> wrapper.openFile(paths),
				status -> {
					if (status == TaskStatus.CANCEL_BY_MEMORY) {
						showHeapUsageBar();
						UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
						return;
					}
					onOpen(paths);
					onFinish.run();
				});
	}

	private void onOpen(List<Path> paths) {
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		initTree();
		update();
		runInitialBackgroundJobs();
		BreakpointManager.init(paths.get(0).getParent());
	}

	private void addTreeCustomNodes() {
		treeRoot.replaceCustomNode(ApkSignature.getApkSignature(wrapper));
	}

	private boolean ensureProjectIsSaved() {
		if (project != null && !project.isSaved() && !project.isInitial()) {
			int res = JOptionPane.showConfirmDialog(
					this,
					NLS.str("confirm.not_saved_message"),
					NLS.str("confirm.not_saved_title"),
					JOptionPane.YES_NO_CANCEL_OPTION);
			if (res == JOptionPane.CANCEL_OPTION) {
				return false;
			}
			if (res == JOptionPane.YES_OPTION) {
				project.save();
			}
		}
		return true;
	}

	private void openProject(Path path) {
		if (!ensureProjectIsSaved()) {
			return;
		}
		JadxProject jadxProject = JadxProject.from(path);
		if (jadxProject == null) {
			JOptionPane.showMessageDialog(
					this,
					NLS.str("msg.project_error"),
					NLS.str("msg.project_error_title"),
					JOptionPane.INFORMATION_MESSAGE);
			jadxProject = new JadxProject();
		}
		updateProject(jadxProject);
		settings.addRecentProject(path);
		List<Path> filePaths = jadxProject.getFilePaths();
		if (filePaths == null) {
			clearTree();
		} else {
			open(filePaths);
		}
	}

	public void updateProject(JadxProject jadxProject) {
		jadxProject.setSettings(settings);
		jadxProject.setMainWindow(this);
		this.project = jadxProject;
		this.wrapper.setProject(jadxProject);
		this.cacheObject.setCommentsIndex(new CommentsIndex(wrapper, cacheObject, jadxProject));
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
			pathString = " [" + projectPath.getParent().toAbsolutePath() + ']';
		}
		setTitle((project.isSaved() ? "" : '*')
				+ project.getName() + pathString + " - " + DEFAULT_TITLE);
	}

	protected void resetCache() {
		cacheObject.reset();
		cacheObject.setJRoot(treeRoot);
		cacheObject.setJadxSettings(settings);

		cacheObject.setIndexService(new IndexService(cacheObject));
		cacheObject.setUsageInfo(new CodeUsageInfo(cacheObject.getNodeCache()));
		cacheObject.setTextIndex(new TextSearchIndex(this));
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
				DecompileTask decompileTask = new DecompileTask(this, wrapper);
				Future<TaskStatus> task = backgroundExecutor.execute(decompileTask);
				task.get();
			} catch (Exception e) {
				LOG.error("Decompile task execution failed", e);
			}
		}
	}

	public void cancelBackgroundJobs() {
		ExecutorService worker = Executors.newSingleThreadExecutor();
		worker.execute(backgroundExecutor::cancelAll);
		worker.shutdown();
	}

	public void reOpenFile() {
		List<Path> openedFile = wrapper.getOpenPaths();
		Map<String, Integer> openTabs = storeOpenTabs();
		if (openedFile != null) {
			open(openedFile, () -> restoreOpenTabs(openTabs));
		}
	}

	@NotNull
	private Map<String, Integer> storeOpenTabs() {
		Map<String, Integer> openTabs = new LinkedHashMap<>();
		for (Map.Entry<JNode, ContentPanel> entry : tabbedPane.getOpenTabs().entrySet()) {
			JavaNode javaNode = entry.getKey().getJavaNode();
			String classRealName = "";
			if (javaNode instanceof JavaClass) {
				JavaClass javaClass = (JavaClass) javaNode;
				classRealName = javaClass.getRawName();
			}
			@Nullable
			JumpPosition position = entry.getValue().getTabbedPane().getCurrentPosition();
			int line = 0;
			if (position != null) {
				line = position.getLine();
			}
			openTabs.put(classRealName, line);
		}
		return openTabs;
	}

	private void restoreOpenTabs(Map<String, Integer> openTabs) {
		for (Map.Entry<String, Integer> entry : openTabs.entrySet()) {
			String classRealName = entry.getKey();
			int position = entry.getValue();
			@Nullable
			JavaClass newClass = wrapper.searchJavaClassByRawName(classRealName);
			if (newClass == null) {
				continue;
			}
			JNode newNode = cacheObject.getNodeCache().makeFrom(newClass);
			tabbedPane.codeJump(new JumpPosition(newNode, position, JumpPosition.getDefPos(newNode)));
		}
	}

	private void saveAll(boolean export) {
		JadxArgs decompilerArgs = wrapper.getArgs();
		if ((!decompilerArgs.isFsCaseSensitive() && !decompilerArgs.isRenameCaseSensitive())
				|| !decompilerArgs.isRenameValid() || !decompilerArgs.isRenamePrintable()) {
			JOptionPane.showMessageDialog(
					this,
					NLS.str("msg.rename_disabled", settings.getLangLocale()),
					NLS.str("msg.rename_disabled_title", settings.getLangLocale()),
					JOptionPane.INFORMATION_MESSAGE);
		}
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setToolTipText(NLS.str("file.save_all_msg"));

		Path currentDirectory = settings.getLastSaveFilePath();
		if (currentDirectory != null) {
			fileChooser.setCurrentDirectory(currentDirectory.toFile());
		}

		int ret = fileChooser.showSaveDialog(mainPanel);
		if (ret == JFileChooser.APPROVE_OPTION) {
			decompilerArgs.setExportAsGradleProject(export);
			if (export) {
				decompilerArgs.setSkipSources(false);
				decompilerArgs.setSkipResources(false);
			} else {
				decompilerArgs.setSkipSources(settings.isSkipSources());
				decompilerArgs.setSkipResources(settings.isSkipResources());
			}
			settings.setLastSaveFilePath(fileChooser.getCurrentDirectory().toPath());
			backgroundExecutor.execute(new ExportTask(this, wrapper, fileChooser.getSelectedFile()));
		}
	}

	public void initTree() {
		treeRoot = new JRoot(wrapper);
		cacheObject.setJRoot(treeRoot);
		treeRoot.setFlatPackages(isFlattenPackage);
		treeModel.setRoot(treeRoot);
		addTreeCustomNodes();
		treeRoot.update();
		reloadTree();
		cacheObject.setJadxSettings(settings);
	}

	private void clearTree() {
		tabbedPane.reset();
		resetCache();
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
		reOpenFile();
	}

	private void nodeClickAction(@Nullable Object obj) {
		try {
			if (obj == null) {
				return;
			}
			if (obj instanceof JResource) {
				JResource res = (JResource) obj;
				ResourceFile resFile = res.getResFile();
				if (resFile != null && JResource.isSupportedForView(resFile.getType())) {
					tabbedPane.showNode(res);
				}
			} else if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				if (node.getRootClass() != null) {
					tabbedPane.codeJump(new JumpPosition(node));
				} else {
					tabbedPane.showNode(node);
				}
			}
		} catch (Exception e) {
			LOG.error("Content loading error", e);
		}
	}

	private void rename(JNode node) {
		RenameDialog.rename(this, node);
	}

	private void treeRightClickAction(MouseEvent e) {
		JNode obj = getJNodeUnderMouse(e);
		if (obj instanceof JPackage) {
			JPackagePopupMenu menu = new JPackagePopupMenu(this, (JPackage) obj);
			menu.show(e.getComponent(), e.getX(), e.getY());
		} else if (obj instanceof JClass || obj instanceof JField || obj instanceof JMethod) {
			JPopupMenu menu = new JPopupMenu();
			JMenuItem jmi = new JMenuItem(NLS.str("popup.rename"));
			jmi.addActionListener(action -> rename(obj));
			menu.add(jmi);
			menu.show(e.getComponent(), e.getX(), e.getY());
		}
	}

	@Nullable
	private JNode getJNodeUnderMouse(MouseEvent mouseEvent) {
		TreePath path = tree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());
		if (path != null) {
			Object obj = path.getLastPathComponent();
			if (obj instanceof JNode) {
				return (JNode) obj;
			}
		}
		return null;
	}

	private void syncWithEditor() {
		ContentPanel selectedContentPanel = tabbedPane.getSelectedCodePanel();
		if (selectedContentPanel == null) {
			return;
		}
		JNode node = selectedContentPanel.getNode();
		if (node.getParent() == null && treeRoot != null) {
			// node not register in tree
			node = treeRoot.searchClassInTree(node);
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
		Action openAction = new AbstractAction(NLS.str("file.open_action"), ICON_OPEN) {
			@Override
			public void actionPerformed(ActionEvent e) {
				openFileOrProject();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.open_action"));
		openAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_O, UiUtils.ctrlButton()));

		Action addFilesAction = new AbstractAction(NLS.str("file.add_files_action"), ICON_ADD_FILES) {
			@Override
			public void actionPerformed(ActionEvent e) {
				addFiles();
			}
		};
		addFilesAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.add_files_action"));

		newProjectAction = new AbstractAction(NLS.str("file.new_project")) {
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

		Action exitAction = new AbstractAction(NLS.str("file.exit"), ICON_CLOSE) {
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
					String preferText = ((AbstractCodeContentPanel) panel).getCodeArea().getSelectedText();
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
				new LogViewer(MainWindow.this).setVisible(true);
			}
		};
		logAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.log"));
		logAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_L,
				UiUtils.ctrlButton() | KeyEvent.SHIFT_DOWN_MASK));

		Action aboutAction = new AbstractAction(NLS.str("menu.about"), ICON_JADX) {
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
		forwardAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_RIGHT, KeyEvent.ALT_DOWN_MASK));

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
		file.add(addFilesAction);
		file.addSeparator();
		file.add(newProjectAction);
		file.add(saveProjectAction);
		file.add(saveProjectAsAction);
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
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					nodeClickAction(getJNodeUnderMouse(e));
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
					setIcon(((JNode) value).getIcon());
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

		JPanel leftPane = new JPanel(new BorderLayout());
		JScrollPane treeScrollPane = new JScrollPane(tree);
		treeScrollPane.setMinimumSize(new Dimension(100, 150));

		leftPane.add(treeScrollPane, BorderLayout.CENTER);
		leftPane.add(progressPane, BorderLayout.PAGE_END);
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
		try (InputStream is = getClass().getResourceAsStream(editorThemePath)) {
			editorTheme = Theme.load(is);
		} catch (Exception e) {
			LOG.error("Can't load editor theme from classpath: {}", editorThemePath);
			try (InputStream is = new FileInputStream(editorThemePath)) {
				editorTheme = Theme.load(is);
			} catch (Exception ex) {
				LOG.error("Can't load editor theme from file: {}", editorThemePath);
			}
		}
	}

	public Theme getEditorTheme() {
		return editorTheme;
	}

	public void loadSettings() {
		Font font = settings.getFont();
		Font largerFont = font.deriveFont(font.getSize() + 2.f);

		setFont(largerFont);
		setEditorTheme(settings.getEditorThemePath());
		tree.setFont(largerFont);
		tree.setRowHeight(-1);

		tabbedPane.loadSettings();
	}

	private void closeWindow() {
		if (!ensureProjectIsSaved()) {
			return;
		}
		settings.setTreeWidth(splitPane.getDividerLocation());
		settings.saveWindowPos(this);
		settings.setMainWindowExtendedState(getExtendedState());
		if (debuggerPanel != null) {
			saveSplittersInfo();
		}
		cancelBackgroundJobs();
		wrapper.close();
		heapUsageBar.reset();
		dispose();

		BreakpointManager.saveAndExit();
		FileUtils.deleteTempRootDir();
		System.exit(0);
	}

	private void saveSplittersInfo() {
		settings.setMainWindowVerticalSplitterLoc(verticalSplitter.getDividerLocation());
		settings.setDebuggerStackFrameSplitterLoc(debuggerPanel.getLeftSplitterLocation());
		settings.setDebuggerVarTreeSplitterLoc(debuggerPanel.getRightSplitterLocation());
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

	public ProgressPanel getProgressPane() {
		return progressPane;
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
			Set<Path> current = new HashSet<>(wrapper.getOpenPaths());
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
