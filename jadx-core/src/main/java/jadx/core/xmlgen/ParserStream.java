package jadx.core.xmlgen;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.jetbrains.annotations.NotNull;

public class ParserStream {

	protected static final Charset STRING_CHARSET_UTF16 = Charset.forName("UTF-16LE");
	protected static final Charset STRING_CHARSET_UTF8 = Charset.forName("UTF-8");

	private static final int[] EMPTY_INT_ARRAY = new int[0];
	private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];

	private final InputStream input;
	private long readPos = 0;

	public ParserStream(@NotNull InputStream inputStream) {
		this.input = inputStream;
	}

	public long getPos() {
		return readPos;
	}

	public int readInt8() throws IOException {
		readPos++;
		return input.read();
	}

	public int readInt16() throws IOException {
		readPos += 2;
		int b1 = input.read();
		int b2 = input.read();
		return (b2 & 0xFF) << 8 | b1 & 0xFF;
	}

	public int readInt32() throws IOException {
		readPos += 4;
		InputStream in = input;
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		return b4 << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | b1 & 0xFF;
	}

	public long readUInt32() throws IOException {
		return readInt32() & 0xFFFFFFFFL;
	}

	public String readString16Fixed(int len) throws IOException {
		String str = new String(readInt8Array(len * 2), STRING_CHARSET_UTF16);
		return str.trim();
	}

	public int[] readInt32Array(int count) throws IOException {
		if (count == 0) {
			return EMPTY_INT_ARRAY;
		}
		int[] arr = new int[count];
		for (int i = 0; i < count; i++) {
			arr[i] = readInt32();
		}
		return arr;
	}

	public byte[] readInt8Array(int count) throws IOException {
		if (count == 0) {
			return EMPTY_BYTE_ARRAY;
		}
		readPos += count;
		byte[] arr = new byte[count];
		int pos = input.read(arr, 0, count);
		while (pos < count) {
			int read = input.read(arr, pos, count - pos);
			if (read == -1) {
				throw new IOException("No data, can't read " + count + " bytes");
			}
			pos += read;
		}
		return arr;
	}

	public void skip(long count) throws IOException {
		readPos += count;
		long pos = input.skip(count);
		while (pos < count) {
			long skipped = input.skip(count - pos);
			if (skipped == -1) {
				throw new IOException("No data, can't skip " + count + " bytes");
			}
			pos += skipped;
		}
	}

	public void checkInt8(int expected, String error) throws IOException {
		int v = readInt8();
		if (v != expected) {
			throwException(error, expected, v);
		}
	}

	public void checkInt16(int expected, String error) throws IOException {
		int v = readInt16();
		if (v != expected) {
			throwException(error, expected, v);
		}
	}

	private void throwException(String error, int expected, int actual) throws IOException {
		throw new IOException(error
				+ ", expected: 0x" + Integer.toHexString(expected)
				+ ", actual: 0x" + Integer.toHexString(actual)
				+ ", offset: 0x" + Long.toHexString(getPos()));
	}

	public void checkPos(long expectedOffset, String error) throws IOException {
		if (getPos() != expectedOffset) {
			throw new IOException(error + ", expected offset: 0x" + Long.toHexString(expectedOffset)
					+ ", actual: 0x" + Long.toHexString(getPos()));
		}
	}

	public void skipToPos(long expectedOffset, String error) throws IOException {
		long pos = getPos();
		if (pos > expectedOffset) {
			throw new IOException(error + ", expected offset not reachable: 0x" + Long.toHexString(expectedOffset)
					+ ", actual: 0x" + Long.toHexString(getPos()));
		}
		if (pos < expectedOffset) {
			skip(expectedOffset - pos);
		}
		checkPos(expectedOffset, error);
	}

	public void mark(int len) throws IOException {
		if (!input.markSupported()) {
			throw new IOException("Mark not supported for input stream " + input.getClass());
		}
		input.mark(len);
	}

	public void reset() throws IOException {
		input.reset();
	}

	public void readFully(byte[] b) throws IOException {
		readFully(b, 0, b.length);
	}

	public void readFully(byte[] b, int off, int len) throws IOException {
		readPos += len;
		if (len < 0) {
			throw new IndexOutOfBoundsException();
		}
		int n = 0;
		while (n < len) {
			int count = input.read(b, off + n, len - n);
			if (count < 0) {
				throw new EOFException();
			}
			n += count;
		}
	}

	@Override
	public String toString() {
		return "pos: 0x" + Long.toHexString(readPos);
	}
}
