package jadx.gui;

import jadx.api.JavaClass;
import jadx.cli.JadxArgs;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRoot;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
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

	public static final String DEFAULT_TITLE = "jadx-gui";
	public static final Color BACKGROUND = new Color(0xf7f7f7);

	private final JadxWrapper wrapper;
	private JPanel mainPanel;
	private JTree tree;
	private DefaultTreeModel treeModel;
	private RSyntaxTextArea textArea;

	public MainWindow(JadxArgs jadxArgs) {
		this.wrapper = new JadxWrapper(jadxArgs);

		initUI();
		initMenu();
	}

	private void initMenu() {
		JMenuBar menuBar = new JMenuBar();

		JMenu file = new JMenu("File");
		file.setMnemonic(KeyEvent.VK_F);

		JMenuItem exit = new JMenuItem("Exit", Utils.openIcon("cross"));
		exit.setMnemonic(KeyEvent.VK_E);
		exit.setToolTipText("Exit application");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		JMenuItem open = new JMenuItem("Open", Utils.openIcon("folder"));
		open.setMnemonic(KeyEvent.VK_E);
		open.setToolTipText("Open file");
		open.addActionListener(new ActionListener() {
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
		});

		file.add(open);
		file.addSeparator();
		file.add(exit);

		menuBar.add(file);
		setJMenuBar(menuBar);
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
			public void valueChanged(TreeSelectionEvent e) {
				Object obj = tree.getLastSelectedPathComponent();
				if (obj instanceof JClass) {
					JavaClass jc = ((JClass) obj).getCls();
					String code = jc.getCode();
					textArea.setText(code);
					textArea.setCaretPosition(0);
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
}
