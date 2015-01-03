package jadx.core.xmlgen;

import java.io.IOException;
import java.io.InputStream;

public class ParserStream {

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

	public byte[] readArray(int count) throws IOException {
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
}
