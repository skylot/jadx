package jadx.api.plugins.utils;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {

	private final long maxSize;

	private long currentPos;

	protected LimitedInputStream(InputStream in, long maxSize) {
		super(in);
		this.maxSize = maxSize;
	}

	private void checkPos() {
		if (currentPos > maxSize) {
			throw new IllegalStateException("Read limit exceeded");
		}
	}

	@Override
	public int read() throws IOException {
		int data = super.read();
		if (data != -1) {
			currentPos++;
			checkPos();
		}
		return data;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = super.read(b, off, len);
		if (count > 0) {
			currentPos += count;
			checkPos();
		}
		return count;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = super.skip(n);
		if (skipped != 0) {
			currentPos += skipped;
			checkPos();
		}
		return skipped;
	}
}
