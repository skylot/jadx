package jadx.gui.ui;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.ResourceFile;
import jadx.gui.JadxWrapper;
import jadx.gui.jobs.BackgroundWorker;
import jadx.gui.jobs.DecompileJob;
import jadx.gui.jobs.IndexJob;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsWindow;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JLoadableNode;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.update.JadxUpdate;
import jadx.gui.update.JadxUpdate.IUpdateCallback;
import jadx.gui.update.data.Release;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.Utils;

import static javax.swing.KeyStroke.getKeyStroke;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";

	private static final double BORDER_RATIO = 0.15;
	private static final double WINDOW_RATIO = 1 - BORDER_RATIO * 2;
	private static final double SPLIT_PANE_RESIZE_WEIGHT = 0.15;

	private static final ImageIcon ICON_OPEN = Utils.openIcon("folder");
	private static final ImageIcon ICON_SAVE_ALL = Utils.openIcon("disk_multiple");
	private static final ImageIcon ICON_EXPORT = Utils.openIcon("database_save");
	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_SYNC = Utils.openIcon("sync");
	private static final ImageIcon ICON_FLAT_PKG = Utils.openIcon("empty_logical_package_obj");
	private static final ImageIcon ICON_SEARCH = Utils.openIcon("wand");
	private static final ImageIcon ICON_FIND = Utils.openIcon("magnifier");
	private static final ImageIcon ICON_BACK = Utils.openIcon("icon_back");
	private static final ImageIcon ICON_FORWARD = Utils.openIcon("icon_forward");
	private static final ImageIcon ICON_PREF = Utils.openIcon("wrench");
	private static final ImageIcon ICON_DEOBF = Utils.openIcon("lock_edit");
	private static final ImageIcon ICON_LOG = Utils.openIcon("report");

	private final transient JadxWrapper wrapper;
	private final transient JadxSettings settings;
	private final transient CacheObject cacheObject;

	private JPanel mainPanel;

	private JTree tree;
	private DefaultTreeModel treeModel;
	private JRoot treeRoot;
	private TabbedPane tabbedPane;

	private boolean isFlattenPackage;
	private JToggleButton flatPkgButton;
	private JCheckBoxMenuItem flatPkgMenuItem;

	private JToggleButton deobfToggleBtn;
	private JCheckBoxMenuItem deobfMenuItem;

	private transient Link updateLink;
	private transient ProgressPanel progressPane;
	private transient BackgroundWorker backgroundWorker;

	public MainWindow(JadxSettings settings) {
		this.wrapper = new JadxWrapper(settings);
		this.settings = settings;
		this.cacheObject = new CacheObject();

		resetCache();
		initUI();
		initMenuAndToolbar();
		checkForUpdate();
	}

	public void open() {
		pack();
		setLocationAndPosition();
		setVisible(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		if (settings.getFiles().isEmpty()) {
			openFile();
		} else {
			openFile(new File(settings.getFiles().get(0)));
		}
	}

	private void checkForUpdate() {
		if (!settings.isCheckForUpdates()) {
			return;
		}
		JadxUpdate.check(new IUpdateCallback() {
			@Override
			public void onUpdate(final Release r) {
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						updateLink.setText(String.format(NLS.str("menu.update_label"), r.getName()));
						updateLink.setVisible(true);
					}
				});
			}
		});
	}

	public void openFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(true);
		String[] exts = {"apk", "dex", "jar", "class", "zip", "aar", "arsc"};
		String description = "supported files: " + Arrays.toString(exts).replace('[', '(').replace(']', ')');
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, exts));
		fileChooser.setToolTipText(NLS.str("file.open"));
		String currentDirectory = settings.getLastOpenFilePath();
		if (!currentDirectory.isEmpty()) {
			fileChooser.setCurrentDirectory(new File(currentDirectory));
		}
		int ret = fileChooser.showDialog(mainPanel, NLS.str("file.open_title"));
		if (ret == JFileChooser.APPROVE_OPTION) {
			settings.setLastOpenFilePath(fileChooser.getCurrentDirectory().getPath());
			openFile(fileChooser.getSelectedFile());
		}
	}

	public void openFile(File file) {
		tabbedPane.closeAllTabs();
		resetCache();
		wrapper.openFile(file);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		settings.addRecentFile(file.getAbsolutePath());
		initTree();
		setTitle(DEFAULT_TITLE + " - " + file.getName());
		runBackgroundJobs();
	}

	protected void resetCache() {
		cacheObject.reset();
		// TODO: decompilation freezes sometime with several threads
		int threadsCount = 1; // settings.getThreadsCount();
		cacheObject.setDecompileJob(new DecompileJob(wrapper, threadsCount));
		cacheObject.setIndexJob(new IndexJob(wrapper, cacheObject, threadsCount));
	}

	private synchronized void runBackgroundJobs() {
		cancelBackgroundJobs();
		backgroundWorker = new BackgroundWorker(cacheObject, progressPane);
		if (settings.isAutoStartJobs()) {
			new Timer().schedule(new TimerTask() {
				@Override
				public void run() {
					backgroundWorker.exec();
				}
			}, 1000);
		}
	}

	public synchronized void cancelBackgroundJobs() {
		if (backgroundWorker != null) {
			backgroundWorker.stop();
			backgroundWorker = new BackgroundWorker(cacheObject, progressPane);
			resetCache();
		}
	}

	public void reOpenFile() {
		File openedFile = wrapper.getOpenFile();
		if (openedFile != null) {
			openFile(openedFile);
		}
	}

	private void saveAll(boolean export) {
		settings.setExportAsGradleProject(export);
		if (export) {
			settings.setSkipSources(false);
			settings.setSkipResources(false);
		}

		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setToolTipText(NLS.str("file.save_all_msg"));

		String currentDirectory = settings.getLastSaveFilePath();
		if (!currentDirectory.isEmpty()) {
			fileChooser.setCurrentDirectory(new File(currentDirectory));
		}

		int ret = fileChooser.showDialog(mainPanel, NLS.str("file.select"));
		if (ret == JFileChooser.APPROVE_OPTION) {
			settings.setLastSaveFilePath(fileChooser.getCurrentDirectory().getPath());
			ProgressMonitor progressMonitor = new ProgressMonitor(mainPanel, NLS.str("msg.saving_sources"), "", 0, 100);
			progressMonitor.setMillisToPopup(0);
			wrapper.saveAll(fileChooser.getSelectedFile(), progressMonitor);
		}
	}

	private void initTree() {
		treeRoot = new JRoot(wrapper);
		treeRoot.setFlatPackages(isFlattenPackage);
		treeModel.setRoot(treeRoot);
		reloadTree();
	}

	private void reloadTree() {
		treeModel.reload();
		tree.expandRow(1);
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

	private void treeClickAction() {
		try {
			Object obj = tree.getLastSelectedPathComponent();
			if (obj instanceof JResource) {
				JResource res = (JResource) obj;
				ResourceFile resFile = res.getResFile();
				if (resFile != null && JResource.isSupportedForView(resFile.getType())) {
					tabbedPane.showResource(res);
				}
			} else if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				JClass cls = node.getRootClass();
				if (cls != null) {
					tabbedPane.codeJump(new Position(cls, node.getLine()));
				}
			}
		} catch (Exception e) {
			LOG.error("Content loading error", e);
		}
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
				openFile();
			}
		};
		openAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.open"));
		openAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_O, KeyEvent.CTRL_DOWN_MASK));

		Action saveAllAction = new AbstractAction(NLS.str("file.save_all"), ICON_SAVE_ALL) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAll(false);
			}
		};
		saveAllAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.save_all"));
		saveAllAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_S, KeyEvent.CTRL_DOWN_MASK));

		Action exportAction = new AbstractAction(NLS.str("file.export_gradle"), ICON_EXPORT) {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAll(true);
			}
		};
		exportAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("file.export_gradle"));
		exportAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK));

		JMenu recentFiles = new JMenu(NLS.str("menu.recent_files"));
		recentFiles.addMenuListener(new RecentFilesMenuListener(recentFiles));

		Action prefsAction = new AbstractAction(NLS.str("menu.preferences"), ICON_PREF) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new JadxSettingsWindow(MainWindow.this, settings).setVisible(true);
			}
		};
		prefsAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.preferences"));
		prefsAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_P,
				KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

		Action exitAction = new AbstractAction(NLS.str("file.exit"), ICON_CLOSE) {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		};

		isFlattenPackage = settings.isFlattenPackage();
		flatPkgMenuItem = new JCheckBoxMenuItem(NLS.str("menu.flatten"), ICON_FLAT_PKG);
		flatPkgMenuItem.setState(isFlattenPackage);

		Action syncAction = new AbstractAction(NLS.str("menu.sync"), ICON_SYNC) {
			@Override
			public void actionPerformed(ActionEvent e) {
				syncWithEditor();
			}
		};
		syncAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.sync"));
		syncAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_T, KeyEvent.CTRL_DOWN_MASK));

		Action textSearchAction = new AbstractAction(NLS.str("menu.text_search"), ICON_SEARCH) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SearchDialog(MainWindow.this, true).setVisible(true);
			}
		};
		textSearchAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.text_search"));
		textSearchAction.putValue(Action.ACCELERATOR_KEY,
				getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

		Action clsSearchAction = new AbstractAction(NLS.str("menu.class_search"), ICON_FIND) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new SearchDialog(MainWindow.this, false).setVisible(true);
			}
		};
		clsSearchAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.class_search"));
		clsSearchAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_N, KeyEvent.CTRL_DOWN_MASK));

		Action deobfAction = new AbstractAction(NLS.str("preferences.deobfuscation"), ICON_DEOBF) {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleDeobfuscation();
			}
		};
		deobfAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("preferences.deobfuscation"));
		deobfAction.putValue(Action.ACCELERATOR_KEY,
				getKeyStroke(KeyEvent.VK_D, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));

		deobfToggleBtn = new JToggleButton(deobfAction);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		deobfToggleBtn.setText("");

		deobfMenuItem = new JCheckBoxMenuItem(deobfAction);
		deobfMenuItem.setState(settings.isDeobfuscationOn());

		Action logAction = new AbstractAction(NLS.str("menu.log"), ICON_LOG) {
			@Override
			public void actionPerformed(ActionEvent e) {
				new LogViewer(settings).setVisible(true);
			}
		};
		logAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("menu.log"));
		logAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_L,
				KeyEvent.CTRL_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK));

		Action aboutAction = new AbstractAction(NLS.str("menu.about")) {
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
		backAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_LEFT,
				KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));

		Action forwardAction = new AbstractAction(NLS.str("nav.forward"), ICON_FORWARD) {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navForward();
			}
		};
		forwardAction.putValue(Action.SHORT_DESCRIPTION, NLS.str("nav.forward"));
		forwardAction.putValue(Action.ACCELERATOR_KEY, getKeyStroke(KeyEvent.VK_RIGHT,
				KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK));

		JMenu file = new JMenu(NLS.str("menu.file"));
		file.setMnemonic(KeyEvent.VK_F);
		file.add(openAction);
		file.add(saveAllAction);
		file.add(exportAction);
		file.addSeparator();
		file.add(recentFiles);
		file.addSeparator();
		file.add(prefsAction);
		file.addSeparator();
		file.add(exitAction);

		JMenu view = new JMenu(NLS.str("menu.view"));
		view.setMnemonic(KeyEvent.VK_V);
		view.add(flatPkgMenuItem);
		view.add(syncAction);

		JMenu nav = new JMenu(NLS.str("menu.navigation"));
		nav.setMnemonic(KeyEvent.VK_N);
		nav.add(textSearchAction);
		nav.add(clsSearchAction);
		nav.addSeparator();
		nav.add(backAction);
		nav.add(forwardAction);

		JMenu tools = new JMenu(NLS.str("menu.tools"));
		tools.setMnemonic(KeyEvent.VK_T);
		tools.add(deobfMenuItem);
		tools.add(logAction);

		JMenu help = new JMenu(NLS.str("menu.help"));
		help.setMnemonic(KeyEvent.VK_H);
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
		toolbar.add(saveAllAction);
		toolbar.add(exportAction);
		toolbar.addSeparator();
		toolbar.add(syncAction);
		toolbar.add(flatPkgButton);
		toolbar.addSeparator();
		toolbar.add(textSearchAction);
		toolbar.add(clsSearchAction);
		toolbar.addSeparator();
		toolbar.add(backAction);
		toolbar.add(forwardAction);
		toolbar.addSeparator();
		toolbar.add(deobfToggleBtn);
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
		mainPanel = new JPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);
		mainPanel.add(splitPane);

		DefaultMutableTreeNode treeRootNode = new DefaultMutableTreeNode(NLS.str("msg.open_file"));
		treeModel = new DefaultTreeModel(treeRootNode);
		tree = new JTree(treeModel);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				treeClickAction();
			}
		});
		tree.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					treeClickAction();
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
				return c;
			}
		});
		tree.addTreeWillExpandListener(new TreeWillExpandListener() {
			@Override
			public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
				TreePath path = event.getPath();
				Object node = path.getLastPathComponent();
				if (node instanceof JLoadableNode) {
					((JLoadableNode) node).loadNode();
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
			}
		});

		progressPane = new ProgressPanel(this, true);

		JPanel leftPane = new JPanel(new BorderLayout());
		leftPane.add(new JScrollPane(tree), BorderLayout.CENTER);
		leftPane.add(progressPane, BorderLayout.PAGE_END);
		splitPane.setLeftComponent(leftPane);

		tabbedPane = new TabbedPane(this);
		splitPane.setRightComponent(tabbedPane);

		new DropTarget(this, DnDConstants.ACTION_COPY, new MainDropTarget(this));

		setContentPane(mainPanel);
		setTitle(DEFAULT_TITLE);
	}

	public void setLocationAndPosition() {
		if (this.settings.loadWindowPos(this)) {
			return;
		}
		GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		DisplayMode mode = gd.getDisplayMode();
		int w = mode.getWidth();
		int h = mode.getHeight();
		setLocation((int) (w * BORDER_RATIO), (int) (h * BORDER_RATIO));
		setSize((int) (w * WINDOW_RATIO), (int) (h * WINDOW_RATIO));
	}

	public void updateFont(Font font) {
		setFont(font);
		tabbedPane.loadSettings();
	}

	@Override
	public void dispose() {
		settings.saveWindowPos(this);
		cancelBackgroundJobs();
		super.dispose();
	}

	public JadxWrapper getWrapper() {
		return wrapper;
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

	public BackgroundWorker getBackgroundWorker() {
		return backgroundWorker;
	}

	private class RecentFilesMenuListener implements MenuListener {
		private final JMenu recentFiles;

		public RecentFilesMenuListener(JMenu recentFiles) {
			this.recentFiles = recentFiles;
		}

		@Override
		public void menuSelected(MenuEvent menuEvent) {
			recentFiles.removeAll();
			File openFile = wrapper.getOpenFile();
			String currentFile = openFile == null ? "" : openFile.getAbsolutePath();
			for (final String file : settings.getRecentFiles()) {
				if (file.equals(currentFile)) {
					continue;
				}
				JMenuItem menuItem = new JMenuItem(file);
				recentFiles.add(menuItem);
				menuItem.addActionListener(e -> openFile(new File(file)));
			}
			if (recentFiles.getItemCount() == 0) {
				recentFiles.add(new JMenuItem(NLS.str("menu.no_recent_files")));
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
