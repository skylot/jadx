package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaClass;
import jadx.api.JavaMethod;
import jadx.api.JavaNode;
import jadx.api.JavaVariable;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.api.data.impl.JadxCodeRef;
import jadx.api.data.impl.JadxCodeRename;
import jadx.api.data.impl.JadxNodeRef;
import jadx.core.deobf.NameMapper;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JField;
import jadx.gui.treemodel.JMethod;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JVariable;
import jadx.gui.ui.MainWindow;
import jadx.gui.ui.TabbedPane;
import jadx.gui.ui.codearea.ClassCodeContentPanel;
import jadx.gui.ui.codearea.CodeArea;
import jadx.gui.ui.panel.ContentPanel;
import jadx.gui.utils.CacheObject;
import jadx.gui.utils.JNodeCache;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.ui.DocumentUpdateListener;
import jadx.gui.utils.ui.NodeLabel;

public class RenameDialog extends JDialog {
	private static final long serialVersionUID = -3269715644416902410L;

	private static final Logger LOG = LoggerFactory.getLogger(RenameDialog.class);

	private final transient MainWindow mainWindow;
	private final transient CacheObject cache;
	private final transient JNode source;
	private final transient JNode node;
	private transient JTextField renameField;
	private transient JButton renameBtn;

	public static boolean rename(MainWindow mainWindow, JNode node) {
		return rename(mainWindow, node, node);
	}

	public static boolean rename(MainWindow mainWindow, JNode source, JNode node) {
		RenameDialog renameDialog = new RenameDialog(mainWindow, source, node);
		UiUtils.uiRun(() -> renameDialog.setVisible(true));
		UiUtils.uiRun(renameDialog::initRenameField); // wait for UI events to propagate
		return true;
	}

	public static JPopupMenu buildRenamePopup(MainWindow mainWindow, JNode node) {
		JMenuItem jmi = new JMenuItem(NLS.str("popup.rename"));
		jmi.addActionListener(action -> RenameDialog.rename(mainWindow, node));
		JPopupMenu menu = new JPopupMenu();
		menu.add(jmi);
		return menu;
	}

	private RenameDialog(MainWindow mainWindow, JNode source, JNode node) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.cache = mainWindow.getCacheObject();
		this.source = source;
		this.node = replaceNode(node);
		initUI();
	}

	private void initRenameField() {
		renameField.setText(node.getName());
		renameField.selectAll();
	}

	private JNode replaceNode(JNode node) {
		if (node instanceof JMethod) {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			if (javaMethod.isClassInit()) {
				throw new JadxRuntimeException("Can't rename class init method: " + node);
			}
			if (javaMethod.isConstructor()) {
				// rename class instead constructor
				return node.getJParent();
			}
		}
		return node;
	}

	private boolean checkNewName() {
		String newName = renameField.getText();
		if (newName.isEmpty()) {
			// use empty name to reset rename (revert to original)
			return true;
		}
		boolean valid = NameMapper.isValidIdentifier(newName);
		if (renameBtn.isEnabled() != valid) {
			renameBtn.setEnabled(valid);
			renameField.putClientProperty("JComponent.outline", valid ? "" : "error");
		}
		return valid;
	}

	private void rename() {
		if (!checkNewName()) {
			return;
		}
		try {
			updateCodeRenames(set -> processRename(node, renameField.getText(), set));
			refreshState();
		} catch (Exception e) {
			LOG.error("Rename failed", e);
			UiUtils.errorMessage(this, "Rename failed:\n" + Utils.getStackTrace(e));
		}
		dispose();
	}

	private void processRename(JNode node, String newName, Set<ICodeRename> renames) {
		JadxCodeRename rename = buildRename(node, newName, renames);
		renames.remove(rename);
		JavaNode javaNode = node.getJavaNode();
		if (javaNode != null) {
			javaNode.removeAlias();
		}
		if (!newName.isEmpty()) {
			renames.add(rename);
		}
	}

	@NotNull
	private JadxCodeRename buildRename(JNode node, String newName, Set<ICodeRename> renames) {
		if (node instanceof JMethod) {
			JavaMethod javaMethod = ((JMethod) node).getJavaMethod();
			List<JavaMethod> relatedMethods = javaMethod.getOverrideRelatedMethods();
			if (!relatedMethods.isEmpty()) {
				for (JavaMethod relatedMethod : relatedMethods) {
					renames.remove(new JadxCodeRename(JadxNodeRef.forMth(relatedMethod), ""));
				}
			}
			return new JadxCodeRename(JadxNodeRef.forMth(javaMethod), newName);
		}
		if (node instanceof JField) {
			return new JadxCodeRename(JadxNodeRef.forFld(((JField) node).getJavaField()), newName);
		}
		if (node instanceof JClass) {
			return new JadxCodeRename(JadxNodeRef.forCls(((JClass) node).getCls()), newName);
		}
		if (node instanceof JPackage) {
			return new JadxCodeRename(JadxNodeRef.forPkg(((JPackage) node).getFullName()), newName);
		}
		if (node instanceof JVariable) {
			JavaVariable javaVar = ((JVariable) node).getJavaVarNode();
			return new JadxCodeRename(JadxNodeRef.forMth(javaVar.getMth()), JadxCodeRef.forVar(javaVar), newName);
		}
		throw new JadxRuntimeException("Failed to build rename node for: " + node);
	}

	private void updateCodeRenames(Consumer<Set<ICodeRename>> updater) {
		JadxProject project = mainWindow.getProject();
		JadxCodeData codeData = project.getCodeData();
		if (codeData == null) {
			codeData = new JadxCodeData();
		}
		Set<ICodeRename> set = new HashSet<>(codeData.getRenames());
		updater.accept(set);
		List<ICodeRename> list = new ArrayList<>(set);
		Collections.sort(list);
		codeData.setRenames(list);
		project.setCodeData(codeData);
		mainWindow.getWrapper().reloadCodeData();
	}

	private void refreshState() {
		mainWindow.getWrapper().reInitRenameVisitor();

		JNodeCache nodeCache = cache.getNodeCache();
		JavaNode javaNode = node.getJavaNode();

		List<JavaNode> toUpdate = new ArrayList<>();
		if (source != null && source != node) {
			toUpdate.add(source.getJavaNode());
		}
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
				.map(JavaNode::getTopParentClass)
				.map(nodeCache::makeFrom)
				.filter(Objects::nonNull)
				.collect(Collectors.toSet());

		LOG.debug("Classes to update: {}", updatedTopClasses);

		refreshTabs(mainWindow.getTabbedPane(), updatedTopClasses);

		if (!updatedTopClasses.isEmpty()) {
			mainWindow.getBackgroundExecutor().execute("Refreshing",
					() -> refreshClasses(updatedTopClasses),
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

	private void refreshClasses(Set<JClass> updatedTopClasses) {
		if (updatedTopClasses.size() < 10) {
			// small batch => reload
			LOG.debug("Classes to reload: {}", updatedTopClasses.size());
			for (JClass cls : updatedTopClasses) {
				try {
					cls.reload(cache);
				} catch (Exception e) {
					LOG.error("Failed to reload class: {}", cls.getFullName(), e);
				}
			}
		} else {
			// big batch => unload
			LOG.debug("Classes to unload: {}", updatedTopClasses.size());
			for (JClass cls : updatedTopClasses) {
				try {
					cls.unload(cache);
				} catch (Exception e) {
					LOG.error("Failed to unload class: {}", cls.getFullName(), e);
				}
			}
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
		renameBtn = new JButton(NLS.str("common_dialog.ok"));
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
		JLabel nodeLabel = NodeLabel.longName(node);
		lbl.setLabelFor(nodeLabel);

		renameField = new JTextField(40);
		renameField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> checkNewName()));
		renameField.addActionListener(e -> rename());
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
