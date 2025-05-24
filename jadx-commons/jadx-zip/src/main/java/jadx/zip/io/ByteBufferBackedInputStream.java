package jadx.zip.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteBufferBackedInputStream extends InputStream {
	private final ByteBuffer buf;
	private int markedPosition = 0;

	public ByteBufferBackedInputStream(ByteBuffer buf) {
		this.buf = buf;
	}

	public int read() throws IOException {
		if (!buf.hasRemaining()) {
			return -1;
		}
		return buf.get() & 0xFF;
	}

	@SuppressWarnings("NullableProblems")
	public int read(byte[] bytes, int off, int len) throws IOException {
		if (!buf.hasRemaining()) {
			return -1;
		}
		int readLen = Math.min(len, buf.remaining());
		buf.get(bytes, off, readLen);
		return readLen;
	}

	@Override
	public boolean markSupported() {
		return true;
	}

	@Override
	public synchronized void mark(int unused) {
		markedPosition = buf.position();
	}

	@Override
	public synchronized void reset() {
		buf.position(markedPosition);
	}
}
