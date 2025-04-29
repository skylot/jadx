package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public interface ByteBufferSelectionListener {
	void selectionChanged(ByteBufferSelectionModel sm, long start, long end);
}
