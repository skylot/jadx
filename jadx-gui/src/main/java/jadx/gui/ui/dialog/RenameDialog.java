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
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JavaNode;
import jadx.api.data.ICodeRename;
import jadx.api.data.impl.JadxCodeData;
import jadx.core.utils.Utils;
import jadx.gui.jobs.TaskStatus;
import jadx.gui.settings.JadxProject;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JRenameNode;
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
	private final transient @Nullable JNode source;
	private final transient JRenameNode node;
	private transient JTextField renameField;
	private transient JButton renameBtn;

	public static boolean rename(MainWindow mainWindow, JNode source, JRenameNode node) {
		RenameDialog renameDialog = new RenameDialog(mainWindow, source, node);
		UiUtils.uiRun(() -> renameDialog.setVisible(true));
		UiUtils.uiRun(renameDialog::initRenameField); // wait for UI events to propagate
		return true;
	}

	public static JPopupMenu buildRenamePopup(MainWindow mainWindow, JRenameNode node) {
		JMenuItem jmi = new JMenuItem(NLS.str("popup.rename"));
		jmi.addActionListener(action -> RenameDialog.rename(mainWindow, null, node));
		JPopupMenu menu = new JPopupMenu();
		menu.add(jmi);
		return menu;
	}

	private RenameDialog(MainWindow mainWindow, JNode source, JRenameNode node) {
		super(mainWindow);
		this.mainWindow = mainWindow;
		this.cache = mainWindow.getCacheObject();
		this.source = source;
		this.node = node.replace();
		initUI();
	}

	private void initRenameField() {
		renameField.setText(node.getName());
		renameField.selectAll();
	}

	private boolean checkNewName(String newName) {
		if (newName.isEmpty()) {
			// use empty name to reset rename (revert to original)
			return true;
		}
		boolean valid = node.isValidName(newName);
		if (renameBtn.isEnabled() != valid) {
			renameBtn.setEnabled(valid);
			renameField.putClientProperty("JComponent.outline", valid ? "" : "error");
		}
		return valid;
	}

	private void rename() {
		String newName = renameField.getText().trim();
		if (!checkNewName(newName)) {
			return;
		}
		try {
			updateCodeRenames(set -> processRename(newName, set));
			refreshState();
		} catch (Exception e) {
			LOG.error("Rename failed", e);
			UiUtils.errorMessage(this, "Rename failed:\n" + Utils.getStackTrace(e));
		}
		dispose();
	}

	private void processRename(String newName, Set<ICodeRename> renames) {
		ICodeRename rename = node.buildCodeRename(newName, renames);
		renames.remove(rename);
		node.removeAlias();
		if (!newName.isEmpty()) {
			renames.add(rename);
		}
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

		List<JavaNode> toUpdate = new ArrayList<>();
		if (source != null && source != node) {
			toUpdate.add(source.getJavaNode());
		}
		node.addUpdateNodes(toUpdate);

		JNodeCache nodeCache = cache.getNodeCache();
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
						node.reload(mainWindow);
					});
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
		NodeLabel nodeLabel = new NodeLabel(node.getTitle());
		nodeLabel.setIcon(node.getIcon());
		if (node instanceof JNode) {
			nodeLabel.disableHtml(((JNode) node).disableHtml());
		}
		lbl.setLabelFor(nodeLabel);

		renameField = new JTextField(40);
		renameField.getDocument().addDocumentListener(new DocumentUpdateListener(ev -> checkNewName(renameField.getText())));
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
