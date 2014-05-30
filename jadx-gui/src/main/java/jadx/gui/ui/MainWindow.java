package jadx.gui.ui;

import jadx.gui.JadxWrapper;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRoot;
import jadx.gui.utils.NLS;
import jadx.gui.utils.Utils;

import javax.swing.ImageIcon;
import javax.swing.JButton;
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
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

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
	private static final ImageIcon ICON_FLAT_PKG = Utils.openIcon("empty_logical_package_obj");
	private static final ImageIcon ICON_SEARCH = Utils.openIcon("magnifier");
	private static final ImageIcon ICON_BACK = Utils.openIcon("icon_back");
	private static final ImageIcon ICON_FORWARD = Utils.openIcon("icon_forward");

	private final JadxWrapper wrapper;

	private JPanel mainPanel;

	private JTree tree;
	private DefaultTreeModel treeModel;
	private TabbedPane tabbedPane;

	public MainWindow(JadxWrapper wrapper) {
		this.wrapper = wrapper;

		initUI();
		initMenuAndToolbar();
	}

	public void openFile() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setAcceptAllFileFilterUsed(true);
		fileChooser.setFileFilter(new FileNameExtensionFilter("supported files", "dex", "apk", "jar"));
		fileChooser.setToolTipText(NLS.str("file.open"));
		int ret = fileChooser.showDialog(mainPanel, NLS.str("file.open"));
		if (ret == JFileChooser.APPROVE_OPTION) {
			openFile(fileChooser.getSelectedFile());
		}
	}

	public void openFile(File file) {
		wrapper.openFile(file);
		initTree();
		setTitle(DEFAULT_TITLE + " - " + file.getName());
	}

	private void saveAllAction() {
		JFileChooser fileChooser = new JFileChooser();
		fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fileChooser.setToolTipText(NLS.str("file.save_all_msg"));
		int ret = fileChooser.showDialog(mainPanel, NLS.str("file.select"));
		if (ret == JFileChooser.APPROVE_OPTION) {

			ProgressMonitor progressMonitor = new ProgressMonitor(mainPanel, "Saving sources", "", 0, 100);
			progressMonitor.setMillisToPopup(500);
			wrapper.saveAll(fileChooser.getSelectedFile(), progressMonitor);
		}
	}

	private void initTree() {
		JRoot treeRoot = new JRoot(wrapper);
		treeModel.setRoot(treeRoot);
		treeModel.reload();
		tree.expandRow(0);
	}

	private void toggleFlattenPackage() {
		Object root = treeModel.getRoot();
		if (root instanceof JRoot) {
			JRoot treeRoot = (JRoot) root;
			treeRoot.setFlatPackages(!treeRoot.isFlatPackages());
			treeModel.reload();
			tree.expandRow(0);
		}
	}

	private void treeClickAction() {
		Object obj = tree.getLastSelectedPathComponent();
		if (obj instanceof JNode) {
			JNode node = (JNode) obj;
			JClass cls = node.getRootClass();
			if (cls != null) {
				tabbedPane.showCode(cls, node.getLine());
			}
		}
	}

	private void toggleSearch() {
		CodePanel codePanel = tabbedPane.getSelectedCodePanel();
		if (codePanel != null) {
			codePanel.getSearchBar().toggle();
		}
	}

	private void initMenuAndToolbar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu file = new JMenu("File");
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

		file.add(open);
		file.add(saveAll);
		file.addSeparator();
		file.add(exit);

		menuBar.add(file);
		setJMenuBar(menuBar);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		final JButton openButton = new JButton(ICON_OPEN);
		openButton.addActionListener(new OpenListener());
		openButton.setToolTipText(NLS.str("file.open"));
		toolbar.add(openButton);

		final JButton saveAllButton = new JButton(ICON_SAVE_ALL);
		saveAllButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllAction();
			}
		});
		saveAllButton.setToolTipText(NLS.str("file.save_all"));
		toolbar.add(saveAllButton);

		toolbar.addSeparator();

		final JToggleButton flatPkgButton = new JToggleButton(ICON_FLAT_PKG);
		flatPkgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleFlattenPackage();
			}
		});
		flatPkgButton.setToolTipText(NLS.str("tree.flatten"));
		toolbar.add(flatPkgButton);
		toolbar.addSeparator();

		final JButton searchButton = new JButton(ICON_SEARCH);
		searchButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleSearch();
			}
		});
		searchButton.setToolTipText(NLS.str("search"));
		toolbar.add(searchButton);

		toolbar.addSeparator();

		final JButton backButton = new JButton(ICON_BACK);
		backButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navBack();
			}
		});
		backButton.setToolTipText(NLS.str("nav.back"));
		toolbar.add(backButton);

		final JButton forwardButton = new JButton(ICON_FORWARD);
		forwardButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				tabbedPane.navForward();
			}
		});
		forwardButton.setToolTipText(NLS.str("nav.forward"));
		toolbar.add(forwardButton);

		mainPanel.add(toolbar, BorderLayout.NORTH);
	}

	private void initUI() {
		mainPanel = new JPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(SPLIT_PANE_RESIZE_WEIGHT);
		mainPanel.add(splitPane);

		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Please open file");
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

	private class OpenListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			openFile();
		}
	}
}
