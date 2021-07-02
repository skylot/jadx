package jadx.gui.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
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

import jadx.api.ICodeWriter;
import jadx.api.JavaClass;
import jadx.api.JavaField;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.core.deobf.DeobfPresets;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.nodes.MethodOverrideAttr;
import jadx.core.dex.nodes.MethodNode;
import jadx.core.dex.nodes.RootNode;
import jadx.core.dex.nodes.VariableNode;
import jadx.core.dex.visitors.RenameVisitor;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxSettings;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JVariable;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;

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
		errorMessage.append(NLS.str("msg.rename_disabled")).append(ICodeWriter.NL);

		JadxSettings settings = mainWindow.getSettings();
		boolean valid = true;
		if (!settings.isDeobfuscationOn()) {
			errorMessage.append(" - ").append(NLS.str("msg.rename_disabled_deobfuscation_disabled")).append(ICodeWriter.NL);
			valid = false;
		}
		if (settings.isDeobfuscationForceSave()) {
			errorMessage.append(" - ").append(NLS.str("msg.rename_disabled_force_rewrite_enabled")).append(ICodeWriter.NL);
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

	private void updateDeobfMap(DeobfPresets deobfPresets, String renameText) {
		if (node instanceof JMethod) {
			MethodNode mthNode = ((JavaMethod) node.getJavaNode()).getMethodNode();
			MethodOverrideAttr overrideAttr = mthNode.get(AType.METHOD_OVERRIDE);
			if (overrideAttr != null) {
				for (MethodNode relatedMth : overrideAttr.getRelatedMthNodes()) {
					deobfPresets.getMthPresetMap().put(relatedMth.getMethodInfo().getRawFullId(), renameText);
				}
			}
			deobfPresets.getMthPresetMap().put(mthNode.getMethodInfo().getRawFullId(), renameText);
		} else if (node instanceof JField) {
			JavaField javaField = (JavaField) node.getJavaNode();
			deobfPresets.getFldPresetMap().put(javaField.getFieldNode().getFieldInfo().getRawFullId(), renameText);
		} else if (node instanceof JClass) {
			JavaClass javaClass = (JavaClass) node.getJavaNode();
			deobfPresets.getClsPresetMap().put(javaClass.getRawName(), renameText);
		} else if (node instanceof JPackage) {
			deobfPresets.getPkgPresetMap().put(((JPackage) node).getFullName(), renameText);
		} else if (node instanceof JVariable) {
			VariableNode varNode = ((JVariable) node).getJavaVarNode().getVariableNode();
			deobfPresets.updateVariableName(varNode, renameText);
		}
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
		DeobfPresets deobfPresets = DeobfPresets.build(root);
		if (deobfPresets == null) {
			return false;
		}
		try {
			deobfPresets.load();
		} catch (Exception e) {
			LOG.error("rename(): readDeobfMap() failed");
			return false;
		}
		updateDeobfMap(deobfPresets, renameText);
		try {
			deobfPresets.save();
		} catch (Exception e) {
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
			if (node instanceof JMethod) {
				toUpdate.addAll(((JMethod) node).getJavaMethod().getOverrideRelatedMethods());
			}
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
					(status) -> {
						if (status == TaskStatus.CANCEL_BY_MEMORY) {
							mainWindow.showHeapUsageBar();
							UiUtils.errorMessage(this, NLS.str("message.memoryLow"));
						}
						if (node instanceof JPackage) {
							mainWindow.getTreeRoot().update();
						}
						mainWindow.reloadTree();
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
			cache.getIndexService().refreshIndex(cls.getCls());
		} catch (Exception e) {
			LOG.error("Failed to reload class: {}", cls.getFullName(), e);
		}
	}

	private void refreshTabs(TabbedPane tabbedPane, Set<JClass> updatedClasses) {
		for (Map.Entry<JNode, ContentPanel> entry : tabbedPane.getOpenTabs().entrySet()) {
			JClass rootClass = entry.getKey().getRootClass();
			if (updatedClasses.remove(rootClass)) {
				ClassCodeContentPanel contentPanel = (ClassCodeContentPanel) entry.getValue();
				CodeArea codeArea = (CodeArea) contentPanel.getJavaCodePanel().getCodeArea();
				codeArea.refreshClass();
			}
		}
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		JButton cancelButton = new JButton(NLS.str("search_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());
		JButton renameBtn = new JButton(NLS.str("common_dialog.ok"));
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
		UiUtils.addEscapeShortCutToDispose(this);
	}

	@Override
	public void dispose() {
		mainWindow.getSettings().saveWindowPos(this);
		super.dispose();
	}
}
