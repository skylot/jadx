package jadx.gui.ui.hexeditor.buffer;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public abstract class ByteBuffer {
	public abstract boolean isEmpty();

	public abstract long length();

	public abstract boolean get(long offset, byte[] dst, int dstOffset, int length);

	public abstract boolean insert(long offset, byte[] src, int srcOffset, int length);

	public abstract boolean overwrite(long offset, byte[] src, int srcOffset, int length);

	public abstract boolean remove(long offset, long length);

	public abstract ByteBuffer slice(long offset, long length);

	public abstract boolean write(OutputStream out, long offset, long length) throws IOException;

	public final boolean transform(ByteTransform tx, long offset, int length) {
		byte[] data = new byte[length];
		return get(offset, data, 0, length)
				&& tx.transform(data, 0, length)
				&& overwrite(offset, data, 0, length);
	}

	public final long indexOf(byte[] pattern) {
		return indexOf(pattern, 0);
	}

	public final long indexOf(byte[] pattern, long index) {
		if (pattern.length == 0) {
			return index;
		}
		int[] shift = new int[256];
		for (int i = 0; i < 256; i++) {
			shift[i] = pattern.length;
		}
		for (int i = 0, n = pattern.length - 1; i < n; i++) {
			shift[pattern[i] & 0xFF] = n - i;
		}
		byte[] text = new byte[pattern.length];
		long maxIndex = length() - pattern.length;
		while (index <= maxIndex) {
			get(index, text, 0, pattern.length);
			if (Arrays.equals(text, pattern)) {
				return index;
			}
			index += shift[text[pattern.length - 1] & 0xFF];
		}
		return -1;
	}

	public final long lastIndexOf(byte[] pattern) {
		return lastIndexOf(pattern, length());
	}

	public final long lastIndexOf(byte[] pattern, long index) {
		if (pattern.length == 0) {
			return index;
		}
		int[] shift = new int[256];
		for (int i = 0; i < 256; i++) {
			shift[i] = pattern.length;
		}
		for (int i = pattern.length - 1; i > 0; i--) {
			shift[pattern[i] & 0xFF] = i;
		}
		byte[] text = new byte[pattern.length];
		index = Math.min(index, length() - pattern.length);
		while (index >= 0) {
			get(index, text, 0, pattern.length);
			if (Arrays.equals(text, pattern)) {
				return index;
			}
			index -= shift[text[0] & 0xFF];
		}
		return -1;
	}

	public final boolean replace(byte[] pattern, byte[] replacement) {
		if (pattern.length == 0) {
			return false;
		}
		boolean changed = false;
		long o = indexOf(pattern);
		while (o >= 0) {
			if (remove(o, pattern.length) && insert(o, replacement, 0, replacement.length)) {
				changed = true;
				o = indexOf(pattern, o + replacement.length);
			} else {
				break;
			}
		}
		return changed;
	}

	private final List<ByteBufferListener> listeners = new ArrayList<ByteBufferListener>();

	public final void addByteBufferListener(ByteBufferListener listener) {
		listeners.add(listener);
	}

	public final void removeByteBufferListener(ByteBufferListener listener) {
		listeners.remove(listener);
	}

	protected final void fireDataInserted(long offset, int length) {
		for (ByteBufferListener l : listeners) {
			l.dataInserted(this, offset, length);
		}
	}

	protected final void fireDataOverwritten(long offset, int length) {
		for (ByteBufferListener l : listeners) {
			l.dataOverwritten(this, offset, length);
		}
	}

	protected final void fireDataRemoved(long offset, long length) {
		for (ByteBufferListener l : listeners) {
			l.dataRemoved(this, offset, length);
		}
	}
}
