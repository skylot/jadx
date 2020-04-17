package jadx.gui.ui;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.dex.nodes.DexNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.visitors.RenameVisitor;
import jadx.core.utils.files.InputFile;
import jadx.gui.jobs.IndexJob;
import jadx.gui.jobs.RefreshJob;
import jadx.gui.jobs.UnloadJob;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.codearea.CodePanel;
import jadx.gui.utils.*;

public class RenameDialog extends CommonSearchDialog {
	private static final long serialVersionUID = -3269715644416902410L;

	private static final Logger LOG = LoggerFactory.getLogger(RenameDialog.class);

	protected final transient MainWindow mainWindow;

	private final transient JNode node;

	private JTextField renameField;

	private CodeArea codeArea;

	private JButton renameBtn;

	public RenameDialog(CodeArea codeArea, JNode node) {
		super(codeArea.getMainWindow());
		mainWindow = codeArea.getMainWindow();
		this.codeArea = codeArea;
		this.node = node;
		if (isDeobfuscationSettingsValid()) {
			initUI();
			registerInitOnOpen();
			loadWindowPos();
		} else {
			LOG.error("Deobfuscation settings are invalid - please enable deobfuscation and disable force rewrite deobfuscation map");
		}
	}

	private boolean isDeobfuscationSettingsValid() {
		boolean valid = true;
		String errorMessage = null;
		JadxSettings settings = mainWindow.getSettings();
		final LangLocale langLocale = settings.getLangLocale();
		if (settings.isDeobfuscationForceSave()) {
			valid = false;
			errorMessage = NLS.str("msg.rename_disabled_force_rewrite_enabled", langLocale);
		}
		if (!settings.isDeobfuscationOn()) {
			valid = false;
			errorMessage = NLS.str("msg.rename_disabled_deobfuscation_disabled", langLocale);
		}
		if (errorMessage != null) {
			showRenameDisabledErrorMessage(langLocale, errorMessage);
		}
		return valid;
	}

	private void showRenameDisabledErrorMessage(LangLocale langLocale, String message) {
		JOptionPane.showMessageDialog(
				mainWindow,
				message,
				NLS.str("msg.rename_disabled_title", langLocale),
				JOptionPane.ERROR_MESSAGE);
	}

	@Override
	protected void openInit() {
		prepare();
	}

	@Override
	protected void loadStart() {
		renameBtn.setEnabled(false);
	}

	@Override
	protected void loadFinished() {
		renameBtn.setEnabled(true);
	}

	private Path getDeobfMapPath(RootNode root) {
		List<DexNode> dexNodes = root.getDexNodes();
		if (dexNodes.isEmpty()) {
			return null;
		}
		InputFile firstInputFile = dexNodes.get(0).getDexFile().getInputFile();
		Path inputFilePath = firstInputFile.getFile().getAbsoluteFile().toPath();

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
			id = javaMethod.getMethodNode().getMethodInfo().getRawFullId();
		} else if (node instanceof JField) {
			JavaField javaField = (JavaField) node.getJavaNode();
			type = "f";
			id = javaField.getFieldNode().getFieldInfo().getRawFullId();
		} else if (node instanceof JClass) {
			type = "c";
			JavaNode javaNode = node.getJavaNode();
			id = javaNode.getFullName();
			if (javaNode instanceof JavaClass) {
				JavaClass javaClass = (JavaClass) javaNode;
				id = javaClass.getRawName();
			}

		} else if (node instanceof JPackage) {
			type = "p";
			id = node.getJavaNode().getFullName();
		}
		return String.format("%s %s = %s", type, id, renameText);
	}

	private boolean writeDeobfMapFile(Path deobfMapPath, List<String> deobfMap) throws IOException {
		if (deobfMapPath == null) {
			LOG.error("updateDeobfMapFile(): deobfMapPath is null!");
			return false;
		}

		File tmpFile = File.createTempFile("deobf_tmp_", ".txt");
		FileOutputStream fileOut = new FileOutputStream(tmpFile);
		for (String entry : deobfMap) {
			fileOut.write(entry.getBytes());
			fileOut.write(System.lineSeparator().getBytes());
		}
		fileOut.close();
		File oldMap = File.createTempFile("deobf_bak_", ".txt");
		Files.copy(deobfMapPath, oldMap.toPath(), StandardCopyOption.REPLACE_EXISTING);
		LOG.trace("Copying " + tmpFile.toPath() + " to " + deobfMapPath);
		Files.copy(tmpFile.toPath(), deobfMapPath, StandardCopyOption.REPLACE_EXISTING);
		Files.delete(oldMap.toPath());
		Files.delete(tmpFile.toPath());
		return true;
	}

	@NotNull
	private List<String> readDeobfMap(Path deobfMapPath) throws IOException {
		return Files.readAllLines(deobfMapPath, StandardCharsets.UTF_8);
	}

	private List<String> updateDeobfMap(List<String> deobfMap, String alias) {
		LOG.trace("updateDeobfMap(): alias = " + alias);
		String id = alias.split("=")[0];
		int i = 0;
		while (i < deobfMap.size()) {
			if (deobfMap.get(i).startsWith(id)) {
				LOG.info("updateDeobfMap(): Removing entry " + deobfMap.get(i));
				deobfMap.remove(i);
			} else {
				i++;
			}
		}
		LOG.trace("updateDeobfMap(): Placing alias = " + alias);
		deobfMap.add(alias);
		return deobfMap;
	}

	private void rename() {
		long start = System.nanoTime();
		String renameText = renameField.getText();
		if (renameText == null || renameText.length() == 0 || codeArea.getText() == null) {
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
		long refreshStart = System.nanoTime();
		int classes = refreshState(root);
		long refreshTime = (System.nanoTime() - refreshStart) / 1000000;
		long totalTime = (System.nanoTime() - start) / 1000000;
		LOG.info("refreshState() took {} ms to update state, {} classes will be refreshed in background ({} ms total)", refreshTime,
				classes,
				totalTime);
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

	private int refreshState(RootNode rootNode) {
		RenameVisitor renameVisitor = new RenameVisitor();
		renameVisitor.init(rootNode);

		cache.getNodeCache().refresh(node);

		Set<JavaClass> updatedClasses = getUpdatedClasses();

		mainWindow.reloadTree();
		refreshTabs(mainWindow.getTabbedPane(), updatedClasses);

		if (updatedClasses.size() > 0) {
			setRefreshTask(updatedClasses);
		}

		return updatedClasses.size();
	}

	private void refreshTabs(TabbedPane tabbedPane, Set<JavaClass> updatedClasses) {
		for (Map.Entry<JNode, ContentPanel> panel : tabbedPane.getOpenTabs().entrySet()) {
			ContentPanel contentPanel = panel.getValue();
			if (contentPanel instanceof ClassCodeContentPanel) {
				JNode node = panel.getKey();
				JClass rootClass = node.getRootClass();
				JavaClass javaClass = rootClass.getCls();
				if (updatedClasses.contains(javaClass) || node.getRootClass().getCls() == javaClass) {
					LOG.info("Refreshing rootClass " + javaClass.getRawName());
					javaClass.unload();
					javaClass.getClassNode().deepUnload();
					rootClass.refresh(); // Update code cache
					ClassCodeContentPanel codePanel = (ClassCodeContentPanel) contentPanel;
					CodePanel javaPanel = codePanel.getJavaCodePanel();
					javaPanel.refresh();
					tabbedPane.refresh(node);
					updatedClasses.remove(javaClass);
				}
			}
		}
	}

	private Set<JavaClass> getUpdatedClasses() {
		Set<JavaClass> usageClasses = new HashSet<>();
		CodeUsageInfo usageInfo = cache.getUsageInfo();
		if (usageInfo != null) {
			usageInfo.getUsageList(node).forEach((node) -> {
				JavaClass rootClass = node.getRootClass().getCls();
				// LOG.info("updateUsages(): Going to update class {}", rootClass.getRealFullName());
				usageClasses.add(rootClass);
			});
			// usageClasses.parallelStream().forEach(JavaClass::refresh);
		}
		return usageClasses;
	}

	private void setRefreshTask(Set<JavaClass> refreshClasses) {
		UnloadJob unloadJob = new UnloadJob(mainWindow.getWrapper(), mainWindow.getSettings().getThreadsCount(), refreshClasses);
		RefreshJob refreshJob = new RefreshJob(mainWindow.getWrapper(), mainWindow.getSettings().getThreadsCount(), refreshClasses);
		LOG.info("Waiting for old unloadJob and refreshJob");
		while (cache.getUnloadJob() != null || cache.getRefreshJob() != null) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return;
			}
		}
		LOG.info("Old unloadJob and refreshJob finished");
		cache.setUnloadJob(unloadJob);
		cache.setRefreshJob(refreshJob);
		cache.setIndexJob(new IndexJob(mainWindow.getWrapper(), cache, mainWindow.getSettings().getThreadsCount()));
		mainWindow.runBackgroundUnloadRefreshAndIndexJobs();
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());
		renameBtn = new JButton(NLS.str("popup.rename"));
		renameBtn.addActionListener(event -> rename());
		getRootPane().setDefaultButton(renameBtn);

		progressPane = new ProgressPanel(mainWindow, false);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(progressPane);
		buttonPane.add(Box.createRigidArea(new Dimension(5, 0)));
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(renameBtn);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPane.add(cancelButton);
		return buttonPane;
	}

	private void initUI() {
		JLabel lbl = new JLabel(NLS.str("popup.rename"));
		JLabel nodeLabel = new JLabel(this.node.makeLongString(), this.node.getIcon(), SwingConstants.LEFT);
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

		warnLabel = new JLabel();
		warnLabel.setForeground(Color.RED);
		warnLabel.setVisible(false);

		JPanel textPane = new JPanel();
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(warnLabel);
		textPane.add(Box.createRigidArea(new Dimension(0, 5)));
		textPane.add(renameField);
		textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		initCommon();
		JPanel buttonPane = initButtonsPanel();

		Container contentPane = getContentPane();
		contentPane.add(renamePane, BorderLayout.PAGE_START);
		contentPane.add(textPane, BorderLayout.CENTER);
		contentPane.add(buttonPane, BorderLayout.PAGE_END);

		setTitle(NLS.str("popup.rename"));
		pack();
		setSize(800, 80);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setModalityType(ModalityType.MODELESS);
	}
}
