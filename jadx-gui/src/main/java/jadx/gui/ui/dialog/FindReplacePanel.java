package jadx.gui.ui.dialog;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jadx.gui.ui.MainWindow;
import jadx.gui.ui.hexeditor.editor.JHexEditor;
import jadx.gui.ui.hexeditor.editor.JHexEditorColors;
import jadx.gui.ui.hexeditor.editor.JHexEditorSuite;
import jadx.gui.utils.NLS;

public class FindReplacePanel extends JPanel {
	private static final long serialVersionUID = 1L;

	private static FindReplacePanel instance = null;

	public static FindReplacePanel getInstance() {
		if (instance == null)
			instance = new FindReplacePanel();
		return instance;
	}

	private final JHexEditorSuite findSuite;
	private final JHexEditor findEditor;
	private JHexEditor editor = null;
	private JDialog dialog = null;

	public FindReplacePanel() {
		JPanel labelPanel = new JPanel(new GridLayout(0, 1, 8, 8));
		labelPanel.add(rightAlign(new JLabel(NLS.str("hex_viewer.find") + ":")));

		JPanel suitePanel = new JPanel(new GridLayout(0, 1, 8, 8));
		suitePanel.add(findSuite = new JHexEditorSuite());
		findEditor = findSuite.getEditor();

		JPanel buttonPanel = new JPanel(new GridLayout(0, 1, 8, 8));
		JButton findNextButton;
		buttonPanel.add(findNextButton = new JButton(NLS.str("hex_viewer.find_next")));
		JButton findPreviousButton;
		buttonPanel.add(findPreviousButton = new JButton(NLS.str("hex_viewer.find_previous")));
		JButton closeButton;
		buttonPanel.add(closeButton = new JButton(NLS.str("common_dialog.cancel")));

		JPanel buttonOuterPanel = new JPanel(new BorderLayout());
		buttonOuterPanel.add(buttonPanel, BorderLayout.PAGE_START);

		setLayout(new BorderLayout(8, 8));
		add(labelPanel, BorderLayout.LINE_START);
		add(suitePanel, BorderLayout.CENTER);
		add(buttonOuterPanel, BorderLayout.LINE_END);
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

		findNextButton.addActionListener(e -> {
			if (editor != null && !findNext(editor)) {
				Toolkit.getDefaultToolkit().beep();
			} else if (dialog != null) {
				dialog.dispose();
			}
		});
		findPreviousButton.addActionListener(e -> {
			if (editor != null && !findPrevious(editor)) {
				Toolkit.getDefaultToolkit().beep();
			} else if (dialog != null) {
				dialog.dispose();
			}
		});

		closeButton.addActionListener(e -> {
			if (dialog != null)
				dialog.dispose();
		});
	}

	public void showDialog(MainWindow mainWindow, JHexEditor rootEditor) {
		findSuite.getInspector().setVisible(false);
		findEditor.setPreferredRowCount(8);
		findEditor.setEnableShortcutKeys(true);
		findEditor.setFont(mainWindow.getSettings().getFont());
		findEditor.setColors(JHexEditorColors.getThemed());

		editor = rootEditor;
		dialog = new JDialog(mainWindow, NLS.str("hex_viewer.find"));
		dialog.setModal(true);
		dialog.setContentPane(this);

		dialog.pack();
		dialog.setResizable(false);
		dialog.setLocationRelativeTo(null);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.setVisible(true);
		dialog = null;
		editor = null;
	}

	public boolean useSelectionForFind(JHexEditor editor) {
		byte[] sel = editor.getSelection();
		if (sel == null || sel.length == 0) {
			return false;
		}
		findEditor.selectAll();
		findEditor.replaceSelection("Use Selection For Find", sel, true);
		return true;
	}

	public byte[] getDataForFind() {
		long length = findEditor.length();
		if (length >= Integer.MAX_VALUE) {
			return null;
		}
		byte[] data = new byte[(int) length];
		findEditor.get(0, data, 0, (int) length);
		return data;
	}

	public boolean findNext(JHexEditor editor) {
		byte[] dataForFind = getDataForFind();
		if (dataForFind == null || dataForFind.length == 0) {
			return false;
		}
		long offset = editor.getSelectionMax();
		offset = editor.indexOf(dataForFind, offset);
		if (offset < 0)
			offset = editor.indexOf(dataForFind);
		if (offset < 0) {
			return false;
		}
		editor.setSelectionRange(offset, offset + dataForFind.length);
		return true;
	}

	public boolean findPrevious(JHexEditor editor) {
		byte[] dataForFind = getDataForFind();
		if (dataForFind == null || dataForFind.length == 0) {
			return false;
		}
		long offset = editor.getSelectionMin() - dataForFind.length;
		offset = editor.lastIndexOf(dataForFind, offset);
		if (offset < 0) {
			offset = editor.lastIndexOf(dataForFind);
		}
		if (offset < 0) {
			return false;
		}
		editor.setSelectionRange(offset, offset + dataForFind.length);
		return true;
	}

	private static JLabel rightAlign(JLabel label) {
		label.setHorizontalAlignment(JLabel.TRAILING);
		return label;
	}
}
