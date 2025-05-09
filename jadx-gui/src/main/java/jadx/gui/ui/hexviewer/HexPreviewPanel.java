package jadx.gui.ui.hexviewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JViewport;

import org.exbin.auxiliary.binary_data.BinaryData;
import org.exbin.auxiliary.binary_data.ByteArrayEditableData;
import org.exbin.bined.CaretMovedListener;
import org.exbin.bined.CodeAreaCaretPosition;
import org.exbin.bined.EditMode;
import org.exbin.bined.basic.BasicCodeAreaZone;
import org.exbin.bined.swing.basic.CodeArea;

import jadx.gui.settings.JadxSettings;

public class HexPreviewPanel extends JPanel {
	private static final long serialVersionUID = 1L;
	public static int CACHE_SIZE = 250;
	private final byte[] valuesCache = new byte[CACHE_SIZE];
	private CodeArea editor;
	private HexEditorHeader header;
	private HexInspectorPanel inspector;

	private JPopupMenu popupMenu;
	private JMenuItem cutAction;
	private JMenuItem copyAction;
	private JMenuItem copyHexAction;
	private JMenuItem copyStringAction;
	private JMenuItem pasteAction;
	private JMenuItem deleteAction;
	private JMenuItem selectAllAction;
	private JMenuItem copyOffsetItem;
	private BasicCodeAreaZone popupMenuPositionZone = BasicCodeAreaZone.UNKNOWN;

	public HexPreviewPanel(JadxSettings settings) {
		new HexPreviewPanel(settings, new CodeArea());
	}

	public HexPreviewPanel(JadxSettings settings, CodeArea editor) {
		this.editor = editor;

		this.header = new HexEditorHeader(editor);
		this.header.setFont(settings.getFont());
		this.inspector = new HexInspectorPanel();

		this.editor.setFont(settings.getFont());
		this.editor.setEditMode(EditMode.READ_ONLY);
		this.editor.setCharset(StandardCharsets.UTF_8);

		this.editor.setComponentPopupMenu(new JPopupMenu() {
			@Override
			public void show(Component invoker, int x, int y) {
				int clickedX = x;
				int clickedY = y;
				if (invoker instanceof JViewport) {
					clickedX += invoker.getParent().getX();
					clickedY += invoker.getParent().getY();
				}
				popupMenuPositionZone = editor.getPainter().getPositionZone(clickedX, clickedY);
				createPopupMenu();

				if (popupMenu != null && popupMenuPositionZone != BasicCodeAreaZone.HEADER
						&& popupMenuPositionZone != BasicCodeAreaZone.ROW_POSITIONS) {
					updatePopupActionStates();
					popupMenu.show(invoker, clickedX, clickedY);
				}

			}
		});

		setLayout(new BorderLayout());
		add(this.editor, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);
		add(inspector, BorderLayout.EAST);

		setFocusable(true);
		addFocusListener(new FocusListener() {
			@Override
			public void focusGained(FocusEvent e) {
				editor.requestFocusInWindow();
			}

			@Override
			public void focusLost(FocusEvent e) {

			}
		});
		createActions();
		enableUpdate();
	}

	public void setData(byte[] data) {
		if (editor != null && data != null) {
			editor.setContentData(new ByteArrayEditableData(data));
			inspector.setBytes(data);
		}
	}

	public void enableUpdate() {
		CaretMovedListener caretMovedListener = (CodeAreaCaretPosition caretPosition) -> updateValues();
		editor.addCaretMovedListener(caretMovedListener);
	}

	private void updateValues() {
		CodeAreaCaretPosition caretPosition = editor.getCaretPosition();
		long dataPosition = caretPosition.getDataPosition();
		long dataSize = editor.getDataSize();

		if (dataPosition < dataSize) {
			int availableData = dataSize - dataPosition >= CACHE_SIZE ? CACHE_SIZE : (int) (dataSize - dataPosition);
			BinaryData contentData = editor.getContentData();
			contentData.copyToArray(dataPosition, valuesCache, 0, availableData);
			if (availableData < CACHE_SIZE) {
				Arrays.fill(valuesCache, availableData, CACHE_SIZE, (byte) 0);
			}
		}

		inspector.setOffset((int) dataPosition);
	}

	private void createActions() {
		cutAction = new JMenuItem("Cut");
		cutAction.addActionListener(e -> performCut());

		copyAction = new JMenuItem("Copy");
		copyAction.addActionListener(e -> performCopy());

		copyHexAction = new JMenuItem("Copy as Hex");
		copyHexAction.addActionListener(e -> performCopyAsCode());

		copyStringAction = new JMenuItem("Copy as String");
		copyStringAction.addActionListener(e -> performCopy());

		pasteAction = new JMenuItem("Paste");
		pasteAction.addActionListener(e -> performPaste());

		deleteAction = new JMenuItem("Delete");
		deleteAction.addActionListener(e -> {
			if (!isEditable()) {
				performDelete();
			}
		});

		selectAllAction = new JMenuItem("Select All");
		selectAllAction.addActionListener(e -> performSelectAll());

		copyOffsetItem = new JMenuItem("Copy Offset");
		copyOffsetItem.addActionListener(e -> copyOffset());
	}

	private void createPopupMenu() {
		boolean isEditable = isEditable();
		popupMenu = new JPopupMenu();
		popupMenu.add(copyAction);

		if (isEditable) {
			popupMenu.add(cutAction);
			popupMenu.add(pasteAction);
			popupMenu.add(deleteAction);
			popupMenu.addSeparator();
		}

		JMenu copyMenu = new JMenu("Copy As...");
		copyMenu.add(copyHexAction);
		copyMenu.add(copyStringAction);
		popupMenu.add(copyMenu);
		popupMenu.add(copyOffsetItem);
		popupMenu.add(selectAllAction);
	}

	private void updatePopupActionStates() {

		boolean selectionExists = isSelection();
		boolean isEditable = !isEditable();

		cutAction.setEnabled(isEditable && selectionExists);
		copyAction.setEnabled(selectionExists);
		copyHexAction.setEnabled(selectionExists);
		copyStringAction.setEnabled(selectionExists);
		deleteAction.setEnabled(isEditable && selectionExists);

		selectAllAction.setEnabled(editor.getDataSize() > 0);
	}

	public CodeArea getEditor() {
		return this.editor;
	}

	public HexEditorHeader getHeader() {
		return this.header;
	}

	public HexInspectorPanel getInspector() {
		return this.inspector;
	}

	public void performCut() {
		editor.cut();
	}

	public void performCopy() {
		editor.copy();
	}

	public void performCopyAsCode() {
		editor.copyAsCode();
	}

	public void performPaste() {
		editor.paste();
	}

	public void performDelete() {
		editor.delete();
	}

	public void performSelectAll() {
		editor.selectAll();
	}

	public boolean isSelection() {
		return editor.hasSelection();
	}

	public boolean isEditable() {
		return editor.isEditable();
	}

	public boolean canPaste() {
		return editor.canPaste();
	}

	public void copyOffset() {
		String s = header.addressString(editor.getSelection().getStart());
		Toolkit tk = Toolkit.getDefaultToolkit();
		Clipboard cb = tk.getSystemClipboard();
		StringSelection ss = new StringSelection(s);
		cb.setContents(ss, OWNER);
	}

	private static final ClipboardOwner OWNER = (cb, t) -> {
		// Nothing.
	};
}
