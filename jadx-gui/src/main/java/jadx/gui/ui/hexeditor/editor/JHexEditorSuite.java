package jadx.gui.ui.hexeditor.editor;

import java.awt.BorderLayout;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

import jadx.gui.ui.hexeditor.buffer.ByteBuffer;
import jadx.gui.ui.hexeditor.buffer.ByteBufferDocument;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class JHexEditorSuite extends JPanel {
	private static final long serialVersionUID = 1L;

	private final JHexEditor editor;
	private final JScrollPane scrollPane;
	private final JHexEditorHeader header;
	private final JHexEditorInspector inspector;

	public JHexEditorSuite() {
		this(new JHexEditor());
	}

	public JHexEditorSuite(byte[] data) {
		this(new JHexEditor(data));
	}

	public JHexEditorSuite(ByteBuffer buffer) {
		this(new JHexEditor(buffer));
	}

	public JHexEditorSuite(ByteBufferDocument document) {
		this(new JHexEditor(document));
	}

	public JHexEditorSuite(JHexEditor editor) {
		this.editor = editor;
		this.scrollPane = new JScrollPane(
				editor,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		this.header = new JHexEditorHeader(editor);
		this.inspector = new JHexEditorInspector(editor);

		setLayout(new BorderLayout());
		add(scrollPane, BorderLayout.CENTER);
		add(header, BorderLayout.NORTH);
		add(inspector, BorderLayout.SOUTH);

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
	}

	public JHexEditor getEditor() {
		return this.editor;
	}

	public JScrollPane getScrollPane() {
		return this.scrollPane;
	}

	public JHexEditorHeader getHeader() {
		return this.header;
	}

	public JHexEditorInspector getInspector() {
		return this.inspector;
	}
}
