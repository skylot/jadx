package jadx.gui.ui.hexeditor.buffer;

import java.io.IOException;
import java.io.OutputStream;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class ArrayByteBuffer extends ByteBuffer {
	private static final int MAX_ARRAY_LENGTH = (1 << 20); // 1MB

	private byte[] array;

	public ArrayByteBuffer() {
		this(0);
	}

	public ArrayByteBuffer(int length) {
		this.array = new byte[length];
	}

	public ArrayByteBuffer(byte[] src) {
		this(src, 0, src.length);
	}

	public ArrayByteBuffer(byte[] src, int offset, int length) {
		this.array = new byte[length];
		for (int i = 0; i < length; i++) {
			this.array[i] = src[offset++];
		}
	}

	@Override
	public boolean isEmpty() {
		return array.length == 0;
	}

	@Override
	public long length() {
		return array.length;
	}

	@Override
	public boolean get(long offset, byte[] dst, int dstOffset, int length) {
		if (length <= 0) {
			return true;
		}
		for (int i = 0; i < length; i++) {
			dst[dstOffset++] = array[(int) offset++];
		}
		return true;
	}

	@Override
	public boolean insert(long offset, byte[] src, int srcOffset, int length) {
		if (length <= 0) {
			return true;
		}
		long newLength = (long) array.length + (long) length;
		if (newLength > MAX_ARRAY_LENGTH) {
			return false;
		}
		byte[] newArray = new byte[(int) newLength];
		int ni = 0;
		for (int i = 0; i < (int) offset; i++) {
			newArray[ni++] = array[i];
		}
		for (int i = 0; i < length; i++) {
			newArray[ni++] = src[srcOffset++];
		}
		for (int i = (int) offset; i < array.length; i++) {
			newArray[ni++] = array[i];
		}
		array = newArray;
		fireDataInserted(offset, length);
		return true;
	}

	@Override
	public boolean overwrite(long offset, byte[] src, int srcOffset, int length) {
		if (length <= 0) {
			return true;
		}
		for (int i = 0; i < length; i++) {
			array[(int) offset++] = src[srcOffset++];
		}
		fireDataOverwritten(offset, length);
		return true;
	}

	@Override
	public boolean remove(long offset, long length) {
		if (length <= 0) {
			return true;
		}
		long newLength = (long) array.length - length;
		if (newLength < 0 || newLength > MAX_ARRAY_LENGTH) {
			return false;
		}
		byte[] newArray = new byte[(int) newLength];
		int ni = 0;
		for (int i = 0; i < (int) offset; i++) {
			newArray[ni++] = array[i];
		}
		for (int i = (int) (offset + length); i < array.length; i++) {
			newArray[ni++] = array[i];
		}
		array = newArray;
		fireDataRemoved(offset, length);
		return true;
	}

	@Override
	public ArrayByteBuffer slice(long offset, long length) {
		return new ArrayByteBuffer(array, (int) offset, (int) length);
	}

	@Override
	public boolean write(OutputStream out, long offset, long length) throws IOException {
		if (length <= 0) {
			return true;
		}
		out.write(array, (int) offset, (int) length);
		return true;
	}
}
