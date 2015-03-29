package jadx.gui.ui;

import jadx.gui.JadxWrapper;
import jadx.gui.settings.JadxSettings;
import jadx.gui.settings.JadxSettingsWindow;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JResource;
import jadx.gui.treemodel.JRoot;
import jadx.gui.update.JadxUpdate;
import jadx.gui.update.JadxUpdate.IUpdateCallback;
import jadx.gui.update.data.Release;
import jadx.gui.utils.Link;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Position;
import jadx.gui.utils.Utils;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JTree;
import javax.swing.ProgressMonitor;
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
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";

	private static final double BORDER_RATIO = 0.15;
	private static final double WINDOW_RATIO = 1 - BORDER_RATIO * 2;
	private static final double SPLIT_PANE_RESIZE_WEIGHT = 0.15;

	private static final ImageIcon ICON_OPEN = Utils.openIcon("folder");
	private static final ImageIcon ICON_SAVE_ALL = Utils.openIcon("disk_multiple");
	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_SYNC = Utils.openIcon("sync");
	private static final ImageIcon ICON_FLAT_PKG = Utils.openIcon("empty_logical_package_obj");
	private static final ImageIcon ICON_SEARCH = Utils.openIcon("wand");
	private static final ImageIcon ICON_FIND = Utils.openIcon("magnifier");
	private static final ImageIcon ICON_BACK = Utils.openIcon("icon_back");
	private static final ImageIcon ICON_FORWARD = Utils.openIcon("icon_forward");
	private static final ImageIcon ICON_PREF = Utils.openIcon("wrench");
	private static final ImageIcon ICON_DEOBF = Utils.openIcon("lock_edit");

	private final JadxWrapper wrapper;
	private final JadxSettings settings;

	private JPanel mainPanel;

	private JTree tree;
	private DefaultTreeModel treeModel;
	private JRoot treeRoot;
	private TabbedPane tabbedPane;

	private JCheckBoxMenuItem flatPkgMenuItem;
	private JToggleButton flatPkgButton;
	private JToggleButton deobfToggleBtn;
	private boolean isFlattenPackage;
	private Link updateLink;

	public MainWindow(JadxSettings settings) {
		this.wrapper = new JadxWrapper(settings);
		this.settings = settings;

		initUI();
		initMenuAndToolbar();
		checkForUpdate();
	}

	public void open() {
		pack();
		setLocationAndPosition();
		setVisible(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		if (settings.getInput().isEmpty()) {
			openFile();
		} else {
			openFile(settings.getInput().get(0));
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
		String[] exts = {"apk", "dex", "jar", "class", "zip"};
		String description = "supported files: " + Arrays.toString(exts).replace('[', '(').replace(']', ')');
		fileChooser.setFileFilter(new FileNameExtensionFilter(description, exts));
		fileChooser.setToolTipText(NLS.str("file.open"));
		String currentDirectory = settings.getLastOpenFilePath();
		if (!currentDirectory.isEmpty()) {
			fileChooser.setCurrentDirectory(new File(currentDirectory));
		}
		int ret = fileChooser.showDialog(mainPanel, NLS.str("file.open"));
		if (ret == JFileChooser.APPROVE_OPTION) {
			settings.setLastOpenFilePath(fileChooser.getCurrentDirectory().getPath());
			openFile(fileChooser.getSelectedFile());
		}
	}

	public void openFile(File file) {
		wrapper.openFile(file);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		settings.addRecentFile(file.getAbsolutePath());
		initTree();
		setTitle(DEFAULT_TITLE + " - " + file.getName());
	}

	public void reOpenFile() {
		File openedFile = wrapper.getOpenFile();
		if (openedFile != null) {
			tabbedPane.closeAllTabs();
			openFile(openedFile);
		}
	}

	private void saveAllAction() {
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
			progressMonitor.setMillisToPopup(500);
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

	private void treeClickAction() {
		try {
			Object obj = tree.getLastSelectedPathComponent();
			if (obj instanceof JResource) {
				JResource res = (JResource) obj;
				if (res.getContent() != null) {
					tabbedPane.showCode(new Position(res, res.getLine()));
				}
			}
			if (obj instanceof JNode) {
				JNode node = (JNode) obj;
				JClass cls = node.getRootClass();
				if (cls != null) {
					tabbedPane.showCode(new Position(cls, node.getLine()));
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
	}

	private void toggleFind() {
		ContentPanel contentPanel = tabbedPane.getSelectedCodePanel();
		if (contentPanel != null) {
			contentPanel.getSearchBar().toggle();
		}
	}

	private void initMenuAndToolbar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu file = new JMenu(NLS.str("menu.file"));
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem exit = new JMenuItem(NLS.str("file.exit"), ICON_CLOSE);
		exit.setMnemonic(KeyEvent.VK_E);
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				dispose();
			}
		});

		JMenuItem open = new JMenuItem(NLS.str("file.open"), ICON_OPEN);
		open.setMnemonic(KeyEvent.VK_O);
		open.addActionListener(new OpenListener());

		JMenuItem saveAll = new JMenuItem(NLS.str("file.save_all"), ICON_SAVE_ALL);
		saveAll.setMnemonic(KeyEvent.VK_S);
		saveAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllAction();
			}
		});

		JMenu recentFiles = new JMenu(NLS.str("menu.recent_files"));
		recentFiles.addMenuListener(new RecentFilesMenuListener(recentFiles));

		JMenuItem preferences = new JMenuItem(NLS.str("menu.preferences"), ICON_PREF);
		ActionListener prefAction = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				final JadxSettingsWindow dialog = new JadxSettingsWindow(MainWindow.this, settings);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dialog.setVisible(true);
					}
				});
			}
		};
		preferences.addActionListener(prefAction);

		file.add(open);
		file.add(saveAll);
		file.addSeparator();
		file.add(recentFiles);
		file.addSeparator();
		file.add(preferences);
		file.addSeparator();
		file.add(exit);

		JMenu view = new JMenu(NLS.str("menu.view"));
		view.setMnemonic(KeyEvent.VK_V);

		isFlattenPackage = settings.isFlattenPackage();

		flatPkgMenuItem = new JCheckBoxMenuItem(NLS.str("menu.flatten"), ICON_FLAT_PKG);
		view.add(flatPkgMenuItem);
		flatPkgMenuItem.setState(isFlattenPackage);

		JMenuItem syncItem = new JMenuItem(NLS.str("menu.sync"), ICON_SYNC);
		view.add(syncItem);
		syncItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				syncWithEditor();
			}
		});

		JMenu nav = new JMenu(NLS.str("menu.navigation"));
		nav.setMnemonic(KeyEvent.VK_N);

		JMenuItem search = new JMenuItem(NLS.str("menu.search"), ICON_SEARCH);
		nav.add(search);
		ActionListener searchAction = new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				final SearchDialog dialog = new SearchDialog(MainWindow.this, tabbedPane, wrapper);
				dialog.prepare();
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						dialog.setVisible(true);
					}
				});
			}
		};
		search.addActionListener(searchAction);

		JMenuItem find = new JMenuItem(NLS.str("menu.find_in_file"), ICON_FIND);
		nav.add(find);
		ActionListener findAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleFind();
			}
		};
		find.addActionListener(findAction);

		JMenu help = new JMenu(NLS.str("menu.help"));
		help.setMnemonic(KeyEvent.VK_H);

		JMenuItem about = new JMenuItem(NLS.str("menu.about"));
		help.add(about);
		about.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				AboutDialog ad = new AboutDialog();
				ad.setVisible(true);
			}
		});

		menuBar.add(file);
		menuBar.add(view);
		menuBar.add(nav);
		menuBar.add(help);
		setJMenuBar(menuBar);

		final JButton openButton = new JButton(ICON_OPEN);
		openButton.addActionListener(new OpenListener());
		openButton.setToolTipText(NLS.str("file.open"));

		final JButton saveAllButton = new JButton(ICON_SAVE_ALL);
		saveAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllAction();
			}
		});
		saveAllButton.setToolTipText(NLS.str("file.save_all"));

		final JButton syncButton = new JButton(ICON_SYNC);
		syncButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				syncWithEditor();
			}
		});
		syncButton.setToolTipText(NLS.str("menu.sync"));

		flatPkgButton = new JToggleButton(ICON_FLAT_PKG);
		flatPkgButton.setSelected(isFlattenPackage);
		ActionListener flatPkgAction = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleFlattenPackage();
			}
		};
		flatPkgButton.addActionListener(flatPkgAction);
		flatPkgMenuItem.addActionListener(flatPkgAction);

		flatPkgButton.setToolTipText(NLS.str("menu.flatten"));

		final JButton searchButton = new JButton(ICON_SEARCH);
		searchButton.addActionListener(searchAction);
		searchButton.setToolTipText(NLS.str("menu.search"));

		final JButton findButton = new JButton(ICON_FIND);
		findButton.addActionListener(findAction);
		findButton.setToolTipText(NLS.str("menu.find_in_file"));

		final JButton backButton = new JButton(ICON_BACK);
		backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navBack();
			}
		});
		backButton.setToolTipText(NLS.str("nav.back"));

		final JButton forwardButton = new JButton(ICON_FORWARD);
		forwardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navForward();
			}
		});
		forwardButton.setToolTipText(NLS.str("nav.forward"));

		final JButton prefButton = new JButton(ICON_PREF);
		prefButton.addActionListener(prefAction);
		prefButton.setToolTipText(NLS.str("menu.preferences"));

		deobfToggleBtn = new JToggleButton(ICON_DEOBF);
		deobfToggleBtn.setSelected(settings.isDeobfuscationOn());
		deobfToggleBtn.setToolTipText(NLS.str("preferences.deobfuscation"));
		deobfToggleBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				settings.setDeobfuscationOn(deobfToggleBtn.isSelected());
				settings.sync();
				reOpenFile();
			}
		});

		updateLink = new Link("", JadxUpdate.JADX_RELEASES_URL);
		updateLink.setVisible(false);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		toolbar.add(openButton);
		toolbar.add(saveAllButton);
		toolbar.addSeparator();

		toolbar.add(syncButton);
		toolbar.add(flatPkgButton);
		toolbar.addSeparator();

		toolbar.add(searchButton);
		toolbar.add(findButton);
		toolbar.addSeparator();

		toolbar.add(backButton);
		toolbar.add(forwardButton);
		toolbar.addSeparator();

		toolbar.add(deobfToggleBtn);
		toolbar.addSeparator();

		toolbar.add(prefButton);
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

		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(NLS.str("msg.open_file"));
		treeModel = new DefaultTreeModel(treeRoot);
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
				if (node instanceof JClass) {
					JClass cls = (JClass) node;
					cls.getRootClass().load();
				}
			}

			@Override
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
			}
		});

		JScrollPane treeScrollPane = new JScrollPane(tree);
		splitPane.setLeftComponent(treeScrollPane);

		tabbedPane = new TabbedPane(this);
		splitPane.setRightComponent(tabbedPane);

		setContentPane(mainPanel);
		setTitle(DEFAULT_TITLE);
	}

	public void setLocationAndPosition() {
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

	public JadxSettings getSettings() {
		return settings;
	}

	private class OpenListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			openFile();
		}
	}

	private class RecentFilesMenuListener implements MenuListener {
		private final JMenu recentFiles;

		public RecentFilesMenuListener(JMenu recentFiles) {
			this.recentFiles = recentFiles;
		}

		@Override
		public void menuSelected(MenuEvent e) {
			recentFiles.removeAll();
			File openFile = wrapper.getOpenFile();
			String currentFile = openFile == null ? "" : openFile.getAbsolutePath();
			for (final String file : settings.getRecentFiles()) {
				if (file.equals(currentFile)) {
					continue;
				}
				JMenuItem menuItem = new JMenuItem(file);
				recentFiles.add(menuItem);
				menuItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						openFile(new File(file));
					}
				});
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
