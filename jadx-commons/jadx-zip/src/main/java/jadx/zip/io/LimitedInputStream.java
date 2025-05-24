package jadx.zip.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class LimitedInputStream extends FilterInputStream {
	private final long maxSize;

	private long currentPos;
	private long markPos;

	public LimitedInputStream(InputStream in, long maxSize) {
		super(in);
		this.maxSize = maxSize;
	}

	private void addAndCheckPos(long count) {
		currentPos += count;
		if (currentPos > maxSize) {
			throw new IllegalStateException("Read limit exceeded");
		}
	}

	@Override
	public int read() throws IOException {
		int data = super.read();
		if (data != -1) {
			addAndCheckPos(1);
		}
		return data;
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int count = super.read(b, off, len);
		if (count > 0) {
			addAndCheckPos(count);
		}
		return count;
	}

	@Override
	public long skip(long n) throws IOException {
		long skipped = super.skip(n);
		if (skipped > 0) {
			addAndCheckPos(skipped);
		}
		return skipped;
	}

	@Override
	public void mark(int readLimit) {
		super.mark(readLimit);
		markPos = currentPos;
	}

	@Override
	public void reset() throws IOException {
		super.reset();
		currentPos = markPos;
	}
}
