package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;

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
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import org.jetbrains.annotations.NotNull;

import jadx.api.metadata.ICodeNodeRef;
import jadx.api.plugins.events.types.NodeRenamedByUser;
import jadx.core.utils.Utils;
import jadx.gui.treemodel.JClass;
import jadx.gui.treemodel.JNode;
import jadx.gui.treemodel.JPackage;
import jadx.gui.treemodel.JRenameNode;
import jadx.gui.ui.MainWindow;
import jadx.gui.utils.NLS;
import jadx.gui.utils.TextStandardActions;
import jadx.gui.utils.UiUtils;
import jadx.gui.utils.pkgs.JRenamePackage;
import jadx.gui.utils.ui.DocumentUpdateListener;
import jadx.gui.utils.ui.NodeLabel;

public class RenameDialog extends JDialog {
	private static final long serialVersionUID = -3269715644416902410L;

	private final transient MainWindow mainWindow;
	private final transient JRenameNode node;
	private transient JTextField renameField;
	private transient JButton renameBtn;

	public static boolean rename(MainWindow mainWindow, JRenameNode node) {
		SwingUtilities.invokeLater(() -> {
			RenameDialog renameDialog = new RenameDialog(mainWindow, node);
			renameDialog.initRenameField();
			renameDialog.setVisible(true);
		});
		return true;
	}

	public static JPopupMenu buildRenamePopup(MainWindow mainWindow, JRenameNode node) {
		JMenuItem jmi = new JMenuItem(NLS.str("popup.rename"));
		jmi.addActionListener(action -> RenameDialog.rename(mainWindow, node));
		jmi.setEnabled(node.canRename());
		JPopupMenu menu = new JPopupMenu();
		menu.add(jmi);
		return menu;
	}

	private RenameDialog(MainWindow mainWindow, JRenameNode node) {
		super(mainWindow);
		this.mainWindow = mainWindow;
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
		rename(renameField.getText().trim());
	}

	private void resetName() {
		rename("");
	}

	private void rename(String newName) {
		if (!checkNewName(newName)) {
			return;
		}
		String oldName = node.getName();
		String newNodeName;
		boolean reset = newName.isEmpty();
		if (reset) {
			node.removeAlias();
			newNodeName = Utils.getOrElse(node.getJavaNode().getName(), "");
		} else {
			newNodeName = newName;
		}
		sendRenameEvent(oldName, newNodeName, reset);
		dispose();
	}

	private void sendRenameEvent(String oldName, String newName, boolean reset) {
		ICodeNodeRef nodeRef = node.getJavaNode().getCodeNodeRef();
		NodeRenamedByUser event = new NodeRenamedByUser(nodeRef, oldName, newName);
		event.setRenameNode(node);
		event.setResetName(reset);
		mainWindow.events().send(event);
	}

	@NotNull
	protected JPanel initButtonsPanel() {
		JButton resetButton = new JButton(NLS.str("common_dialog.reset"));
		resetButton.addActionListener(event -> resetName());

		JButton cancelButton = new JButton(NLS.str("common_dialog.cancel"));
		cancelButton.addActionListener(event -> dispose());

		renameBtn = new JButton(NLS.str("common_dialog.ok"));
		renameBtn.addActionListener(event -> rename());
		getRootPane().setDefaultButton(renameBtn);

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPane.add(resetButton);
		buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
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
		} else if (node instanceof JRenamePackage) {
			// TODO: get from JRenameNode directly
			nodeLabel.disableHtml(!node.getTitle().equals(JPackage.PACKAGE_DEFAULT_HTML_STR));
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
		renamePane.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));

		JPanel textPane = new JPanel();
		textPane.setLayout(new BoxLayout(textPane, BoxLayout.PAGE_AXIS));
		textPane.add(renameField);
		if (node instanceof JClass) {
			textPane.add(new JLabel(NLS.str("rename_dialog.class_help")));
		}
		textPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));

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
