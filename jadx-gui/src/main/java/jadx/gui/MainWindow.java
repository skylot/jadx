package jadx.gui;

import jadx.cli.JadxArgs;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRoot;

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
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.BadLocationException;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainWindow extends JFrame {
	private static final Logger LOG = LoggerFactory.getLogger(MainWindow.class);

	private static final String DEFAULT_TITLE = "jadx-gui";
	private static final Color BACKGROUND = new Color(0xf7f7f7);

	private static final ImageIcon ICON_OPEN = Utils.openIcon("folder");
	private static final ImageIcon ICON_CLOSE = Utils.openIcon("cross");
	private static final ImageIcon ICON_FLAT_PKG = Utils.openIcon("empty_logical_package_obj");

	private final JadxWrapper wrapper;
	private JPanel mainPanel;
	private JTree tree;
	private DefaultTreeModel treeModel;
	private RSyntaxTextArea textArea;

	public MainWindow(JadxArgs jadxArgs) {
		this.wrapper = new JadxWrapper(jadxArgs);

		initUI();
		initMenuAndToolbar();
	}

	public void openFile(File file) {
		wrapper.openFile(file);
		initTree();
		setTitle(DEFAULT_TITLE + " - " + file.getName());
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
			if (node.getJParent() != null) {
				textArea.setText(node.getJParent().getCode());
				scrollToLine(node.getLine());
			}
		}
	}

	private void scrollToLine(int line) {
		if (line < 2) {
			return;
		}
		try {
			textArea.setCaretPosition(textArea.getLineStartOffset(line - 1));
		} catch (BadLocationException e) {
			LOG.error("Can't scroll to " + line, e);
		}
	}

	private void initMenuAndToolbar() {
		JMenuBar menuBar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem exit = new JMenuItem("Exit", ICON_CLOSE);
		exit.setMnemonic(KeyEvent.VK_E);
		exit.setToolTipText("Exit application");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		JMenuItem open = new JMenuItem("Open", ICON_OPEN);
		open.setMnemonic(KeyEvent.VK_E);
		open.setToolTipText("Open file");
		open.addActionListener(new OpenListener());

		file.add(open);
		file.addSeparator();
		file.add(exit);

		menuBar.add(file);
		setJMenuBar(menuBar);

		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);

		JButton openButton = new JButton(ICON_OPEN);
		openButton.addActionListener(new OpenListener());
		openButton.setToolTipText(NLS.str("file.open"));

		toolbar.add(openButton);
		toolbar.addSeparator();

		JToggleButton flatPkgButton = new JToggleButton(ICON_FLAT_PKG);
		flatPkgButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				toggleFlattenPackage();
			}
		});
		flatPkgButton.setToolTipText(NLS.str("tree.flatten"));
		toolbar.add(flatPkgButton);

		toolbar.addSeparator();

		add(toolbar, BorderLayout.NORTH);
	}

	private void initUI() {
		mainPanel = new JPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane();
		mainPanel.add(splitPane);

		DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode("Please open file");
		treeModel = new DefaultTreeModel(treeRoot);
		tree = new JTree(treeModel);
//		tree.setRootVisible(false);
//		tree.setBackground(BACKGROUND);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent event) {
				treeClickAction();
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

		JScrollPane treeScrollPane = new JScrollPane(tree);
		splitPane.setLeftComponent(treeScrollPane);

		textArea = new RSyntaxTextArea(20, 60);
		textArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVA);
		textArea.setMarkOccurrences(true);
		textArea.setBackground(BACKGROUND);
		textArea.setCodeFoldingEnabled(true);
		textArea.setAntiAliasingEnabled(true);
		// textArea.setHyperlinksEnabled(true);
		textArea.setTabSize(4);
		RTextScrollPane scrollPane = new RTextScrollPane(textArea);
		scrollPane.setFoldIndicatorEnabled(true);
		splitPane.setRightComponent(scrollPane);

		setContentPane(mainPanel);
		setTitle(DEFAULT_TITLE);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
	}

	private class OpenListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			JFileChooser fileChooser = new JFileChooser();
			FileFilter filter = new FileNameExtensionFilter("dex files", "dex", "apk", "jar");
			fileChooser.addChoosableFileFilter(filter);
			int ret = fileChooser.showDialog(mainPanel, "Open file");
			if (ret == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				openFile(file);
			}
		}
	}
}
