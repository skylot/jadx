package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class ParserStream {

	protected static final Charset STRING_CHARSET_UTF16 = Charset.forName("UTF-16LE");
	protected static final Charset STRING_CHARSET_UTF8 = Charset.forName("UTF-8");

	private final InputStream input;
	private long readPos = 0;

	public ParserStream(InputStream inputStream) {
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
		return (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	public int readInt32() throws IOException {
		readPos += 4;
		InputStream in = input;
		int b1 = in.read();
		int b2 = in.read();
		int b3 = in.read();
		int b4 = in.read();
		return b4 << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | (b1 & 0xFF);
	}

	public long readUInt32() throws IOException {
		return readInt32() & 0xFFFFFFFFL;
	}

	public String readString8Fixed(int len) throws IOException {
		String str = new String(readInt8Array(len), STRING_CHARSET_UTF8);
		return str.trim();
	}

	public String readString16Fixed(int len) throws IOException {
		String str = new String(readInt8Array(len * 2), STRING_CHARSET_UTF16);
		return str.trim();
	}

	public String readString8() throws IOException {
		decodeLength8();
		int len = decodeLength8();
		String str = new String(readInt8Array(len), STRING_CHARSET_UTF8);
		checkInt8(0, "Not a trailing zero at string8 end");
		return str;
	}

	public String readString16() throws IOException {
		int len = decodeLength16();
		String str = new String(readInt8Array(len), STRING_CHARSET_UTF16);
		checkInt16(0, "Not a trailing zero at string16 end");
		return str;
	}

	public int[] readInt32Array(int count) throws IOException {
		if (count == 0) {
			return new int[0];
		}
		int[] arr = new int[count];
		for (int i = 0; i < count; i++) {
			arr[i] = readInt32();
		}
		return arr;
	}

	public byte[] readInt8Array(int count) throws IOException {
		if (count == 0) {
			return new byte[0];
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
		if (pos < expectedOffset) {
			skip(expectedOffset - pos);
		}
		checkPos(expectedOffset, error);
	}

	public int decodeLength8() throws IOException {
		int len = readInt8();
		if ((len & 0x80) != 0) {
			len = ((len & 0x7F) << 8) | readInt8();
		}
		return len;
	}

	public int decodeLength16() throws IOException {
		int len = readInt16();
		if ((len & 0x8000) != 0) {
			len = ((len & 0x7FFF) << 16) | readInt16();
		}
		return len;
	}

	@Override
	public String toString() {
		return "pos: 0x" + Long.toHexString(readPos);
	}
}
