package jadx.gui;

import jadx.api.JavaClass;
import jadx.api.JavaPackage;
import jadx.cli.JadxArgs;
import jadx.gui.model.JClass;
import jadx.core.utils.exceptions.JadxRuntimeException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.net.URL;

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
	private DefaultMutableTreeNode treeRoot;
	private JTree tree;
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

		JMenuItem exit = new JMenuItem("Exit", openIcon("application-exit"));
		exit.setMnemonic(KeyEvent.VK_E);
		exit.setToolTipText("Exit application");
		exit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		JMenuItem open = new JMenuItem("Open", openIcon("document-open-5"));
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
		treeRoot.removeAllChildren();
		for (JavaPackage pkg : wrapper.getPackages()) {
			MutableTreeNode child = new DefaultMutableTreeNode(pkg);
			int i = 0;
			for (JavaClass javaClass : pkg.getClasses()) {
				MutableTreeNode cls = new DefaultMutableTreeNode(new JClass(javaClass));
				child.insert(cls, i++);
			}
			treeRoot.add(child);
		}
		tree.expandRow(0);
	}

	private ImageIcon openIcon(String name) {
		String iconPath = "/icons-16/" + name + ".png";
		URL resource = getClass().getResource(iconPath);
		if (resource == null) {
			throw new JadxRuntimeException("Icon not found: " + iconPath);
		}
		return new ImageIcon(resource);
	}

	private void initUI() {
		mainPanel = new JPanel(new BorderLayout());
		JSplitPane splitPane = new JSplitPane();
		mainPanel.add(splitPane);

		treeRoot = new DefaultMutableTreeNode("Please open file");
		tree = new JTree(treeRoot);
//		tree.setRootVisible(false);
//		tree.setBackground(BACKGROUND);
		tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			@Override
			public void valueChanged(TreeSelectionEvent e) {
				Object node = tree.getLastSelectedPathComponent();
				if (node instanceof DefaultMutableTreeNode) {
					Object obj = ((DefaultMutableTreeNode) node).getUserObject();
					if (obj instanceof JClass) {
						JavaClass jc = ((JClass) obj).getCls();
						String code = jc.getCode();
						textArea.setText(code);
						textArea.setCaretPosition(0);
					}
				}
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
