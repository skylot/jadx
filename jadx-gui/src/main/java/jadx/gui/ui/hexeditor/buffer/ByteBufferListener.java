package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public interface ByteBufferListener {
	void dataInserted(ByteBuffer buffer, long offset, int length);

	void dataOverwritten(ByteBuffer buffer, long offset, int length);

	void dataRemoved(ByteBuffer buffer, long offset, long length);
}
