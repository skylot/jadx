package jadx.gui.ui.hexeditor.editor;

import jadx.gui.ui.hexeditor.buffer.ByteBufferDocument;
import jadx.gui.ui.hexeditor.buffer.ByteBufferListener;
import jadx.gui.ui.hexeditor.buffer.ByteBufferSelectionListener;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public interface JHexEditorListener extends ByteBufferListener, ByteBufferSelectionListener {
	void documentChanged(JHexEditor editor, ByteBufferDocument document);

	void colorsChanged(JHexEditor editor, JHexEditorColors colors);

	void editorStatusChanged(JHexEditor editor);
}
