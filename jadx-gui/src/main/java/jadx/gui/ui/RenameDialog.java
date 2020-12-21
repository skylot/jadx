package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.codegen.CodeWriter;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.RenameVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.IndexJob;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodePanel;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;

public class RenameDialog extends JDialog {
	private static final long serialVersionUID = -3269715644416902410L;

	private static final Logger LOG = LoggerFactory.getLogger(RenameDialog.class);

	private final transient MainWindow mainWindow;
	private final transient CacheObject cache;
	private final transient JNode node;
	private transient JTextField renameField;

	public static boolean rename(MainWindow mainWindow, JNode node) {
		if (!checkSettings(mainWindow)) {
			return false;
		}
		RenameDialog renameDialog = new RenameDialog(mainWindow, node);
		renameDialog.setVisible(true);
		return true;
	}

	private RenameDialog(MainWindow mainWindow, JNode node) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.cache = mainWindow.getCacheObject();
		this.node = node;
		initUI();
	}

	public static boolean checkSettings(MainWindow mainWindow) {
		StringBuilder errorMessage = new StringBuilder();
		errorMessage.append(NLS.str("msg.rename_disabled")).append(CodeWriter.NL);

		JadxSettings settings = mainWindow.getSettings();
		boolean valid = true;
		if (!settings.isDeobfuscationOn()) {
			errorMessage.append(" - ").append(NLS.str("msg.rename_disabled_deobfuscation_disabled")).append(CodeWriter.NL);
			valid = false;
		}
		if (settings.isDeobfuscationForceSave()) {
			errorMessage.append(" - ").append(NLS.str("msg.rename_disabled_force_rewrite_enabled")).append(CodeWriter.NL);
			valid = false;
		}
		if (valid) {
			return true;
		}
		int result = JOptionPane.showConfirmDialog(mainWindow, errorMessage.toString(),
				NLS.str("msg.rename_disabled_title"), JOptionPane.OK_CANCEL_OPTION);
		if (result != JOptionPane.OK_OPTION) {
			return false;
		}
		settings.setDeobfuscationOn(true);
		settings.setDeobfuscationForceSave(false);
		settings.sync();

		mainWindow.reOpenFile();
		return false; // TODO: can't open dialog, 'node' is replaced with new one after reopen
	}

	private Path getDeobfMapPath(RootNode root) {
		List<File> inputFiles = root.getArgs().getInputFiles();
		if (inputFiles.isEmpty()) {
			return null;
		}
		File firstInputFile = inputFiles.get(0);
		Path inputFilePath = firstInputFile.getAbsoluteFile().toPath();

		String inputName = inputFilePath.getFileName().toString();
		String baseName = inputName.substring(0, inputName.lastIndexOf('.'));
		return inputFilePath.getParent().resolve(baseName + ".jobf");
	}

	private String getNodeAlias(String renameText) {
		String type = "";
		String id = "";
		if (node instanceof JMethod) {
			JavaMethod javaMethod = (JavaMethod) node.getJavaNode();
			type = "m";
			MethodNode mthNode = javaMethod.getMethodNode();
			MethodOverrideAttr overrideAttr = mthNode.get(AType.METHOD_OVERRIDE);
			if (overrideAttr != null) {
				// use method closest to base method
				mthNode = Objects.requireNonNull(Utils.last(overrideAttr.getRelatedMthNodes()));
			}
			id = mthNode.getMethodInfo().getRawFullId();
		} else if (node instanceof JField) {
			JavaField javaField = (JavaField) node.getJavaNode();
			type = "f";
			id = javaField.getFieldNode().getFieldInfo().getRawFullId();
		} else if (node instanceof JClass) {
			JavaClass javaClass = (JavaClass) node.getJavaNode();
			type = "c";
			id = javaClass.getRawName();
		} else if (node instanceof JPackage) {
			type = "p";
			id = ((JPackage) node).getFullName();
		}
		return String.format("%s %s = %s", type, id, renameText);
	}

	private void writeDeobfMapFile(Path deobfMapPath, List<String> deobfMap) throws IOException {
		if (deobfMapPath == null) {
			LOG.error("updateDeobfMapFile(): deobfMapPath is null!");
			return;
		}
		Path deobfMapDir = deobfMapPath.getParent();
		Path tmpFile = Files.createTempFile(deobfMapDir, "deobf_tmp_", ".txt");

		try (Writer writer = Files.newBufferedWriter(tmpFile, StandardCharsets.UTF_8)) {
			for (String entry : deobfMap) {
				writer.write(entry);
				writer.write(System.lineSeparator());
			}
		}
		Files.move(tmpFile, deobfMapPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		LOG.info("Updated deobf file {}", deobfMapPath);
	}

	@NotNull
	private List<String> readDeobfMap(Path deobfMapPath) throws IOException {
		return Files.readAllLines(deobfMapPath, StandardCharsets.UTF_8);
	}

	private List<String> updateDeobfMap(List<String> deobfMap, String alias) {
		String id = alias.substring(0, alias.indexOf('=') + 1);
		int i = 0;
		while (i < deobfMap.size()) {
			String entry = deobfMap.get(i);
			if (entry.startsWith(id)) {
				LOG.debug("updateDeobfMap(): Removing entry {}", entry);
				deobfMap.remove(i);
			} else {
				i++;
			}
		}
		LOG.debug("updateDeobfMap(): placing alias = {}", alias);
		deobfMap.add(alias);
		return deobfMap;
	}

	private void rename() {
		try {
			String renameText = renameField.getText();
			if (renameText == null || renameText.length() == 0) {
				return;
			}
			RootNode root = mainWindow.getWrapper().getDecompiler().getRoot();
			if (node == null) {
				LOG.error("rename(): rootNode is null!");
				dispose();
				return;
			}
			if (!refreshDeobfMapFile(renameText, root)) {
				LOG.error("rename(): refreshDeobfMapFile() failed!");
				dispose();
				return;
			}
			refreshState(root);
		} catch (Exception e) {
			LOG.error("Rename failed", e);
		}
		dispose();
	}

	private boolean refreshDeobfMapFile(String renameText, RootNode root) {
		List<String> deobfMap;
		Path deobfMapPath = getDeobfMapPath(root);
		try {
			deobfMap = readDeobfMap(deobfMapPath);
		} catch (IOException e) {
			LOG.error("rename(): readDeobfMap() failed");
			return false;
		}
		updateDeobfMap(deobfMap, getNodeAlias(renameText));
		try {
			writeDeobfMapFile(deobfMapPath, deobfMap);
		} catch (IOException e) {
			LOG.error("rename(): writeDeobfMap() failed");
			return false;
		}
		return true;
	}

	private void refreshState(RootNode rootNode) {
		RenameVisitor renameVisitor = new RenameVisitor();
		renameVisitor.init(rootNode);

		JNodeCache nodeCache = cache.getNodeCache();
		JavaNode javaNode = node.getJavaNode();

		List<JavaNode> toUpdate = new ArrayList<>();
		if (javaNode != null) {
			toUpdate.add(javaNode);
			toUpdate.addAll(javaNode.getUseIn());
		} else if (node instanceof JPackage) {
			processPackage(toUpdate);
		} else {
			throw new JadxRuntimeException("Unexpected node type: " + node);
		}
		Set<JClass> updatedTopClasses = toUpdate
				.stream()
				.map(nodeCache::makeFrom)
				.map(JNode::getRootClass)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		LOG.debug("Classes to update: {}", updatedTopClasses);

		refreshTabs(mainWindow.getTabbedPane(), updatedTopClasses);

		if (!updatedTopClasses.isEmpty()) {
			mainWindow.getBackgroundExecutor().execute("Refreshing",
					Utils.collectionMap(updatedTopClasses, cls -> () -> refreshJClass(cls)),
					() -> {
						if (node instanceof JPackage) {
							// reinit tree
							mainWindow.initTree();
						} else {
							mainWindow.reloadTree();
						}
					});
		}
	}

	private void processPackage(List<JavaNode> toUpdate) {
		String rawFullPkg = ((JPackage) node).getFullName();
		String rawFullPkgDot = rawFullPkg + ".";
		for (JavaClass cls : mainWindow.getWrapper().getClasses()) {
			String clsPkg = cls.getClassNode().getClassInfo().getPackage();
			// search all classes in package
			if (clsPkg.equals(rawFullPkg) || clsPkg.startsWith(rawFullPkgDot)) {
				toUpdate.add(cls);
				// also include all usages (for import fix)
				toUpdate.addAll(cls.getUseIn());
			}
		}
	}

	private void refreshJClass(JClass cls) {
		try {
			cls.reload();
			IndexJob.refreshIndex(cache, cls.getCls());
		} catch (Exception e) {
			LOG.error("Failed to reload class: {}", cls, e);
		}
	}

	private void refreshTabs(TabbedPane tabbedPane, Set<JClass> updatedClasses) {
		for (Map.Entry<JNode, ContentPanel> entry : tabbedPane.getOpenTabs().entrySet()) {
			ContentPanel contentPanel = entry.getValue();
			if (contentPanel instanceof ClassCodeContentPanel) {
				JNode node = entry.getKey();
				JClass rootClass = node.getRootClass();
				if (updatedClasses.contains(rootClass)) {
					refreshJClass(rootClass);
					ClassCodeContentPanel codePanel = (ClassCodeContentPanel) contentPanel;
					CodePanel javaPanel = codePanel.getJavaCodePanel();
					javaPanel.refresh();
					tabbedPane.refresh(rootClass);
				}
			}
		}
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());
		JButton renameBtn = new JButton(NLS.str("popup.rename"));
		renameBtn.addActionListener(event -> rename());
		getRootPane().setDefaultButton(renameBtn);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(renameBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	private void initUI() {
		JLabel lbl = new JLabel(NLS.str("popup.rename"));
		JLabel nodeLabel = new JLabel(this.node.makeLongStringHtml(), this.node.getIcon(), SwingConstants.LEFT);
		lbl.setLabelFor(nodeLabel);

		renameField = new JTextField(40);
		renameField.addActionListener(e -> rename());
		renameField.setText(node.getName());
		renameField.selectAll();
		new TextStandardActions(renameField);

		JPanel renamePane = new JPanel();
		renamePane.setLayout(new FlowLayout(FlowLayout.LEFT));
		renamePane.add(lbl);
		renamePane.add(nodeLabel);
		renamePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel textPane = new JPanel();
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(Box.createRigidArea(new Dimension(0, 5)));
		textPane.add(renameField);
		textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(renamePane, BorderLayout.PAGE_START);
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		setTitle(NLS.str("popup.rename"));
		if (!mainWindow.getSettings().loadWindowPos(this)) {
			setSize(800, 80);
		}
		// always pack (ignore saved windows sizes)
		pack();
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.APPLICATION_MODAL);
	}

	@Override
	public void dispose() {
		mainWindow.getSettings().saveWindowPos(this);
		super.dispose();
	}
}
