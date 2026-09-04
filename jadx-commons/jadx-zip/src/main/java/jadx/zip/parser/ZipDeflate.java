package jadx.zip.parser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static jadx.zip.parser.JadxZipParser.bufferToStream;

final class ZipDeflate {
	private static final int BUFFER_SIZE = 4096;

	static byte[] decompressEntryToBytes(ByteBuffer buf, JadxZipEntry entry) throws DataFormatException {
		buf.position(entry.getDataStart());
		ByteBuffer entryBuf = buf.slice();
		entryBuf.limit((int) entry.getCompressedSize());
		if (entry.getUncompressedSize() > Integer.MAX_VALUE) {
			throw new DataFormatException("Entry too large: " + entry.getUncompressedSize());
		}
		byte[] out = new byte[(int) entry.getUncompressedSize()];
		int written;
		Inflater inflater = new Inflater(true);
		try {
			inflater.setInput(entryBuf);
			written = inflater.inflate(out);
		} finally {
			inflater.end();
		}
		if (written != out.length) {
			throw new DataFormatException("Unexpected size of decompressed entry: " + entry
					+ ", got: " + written + ", expected: " + out.length);
		}
		return out;
	}

	static InputStream decompressEntryToStream(ByteBuffer buf, JadxZipEntry entry) {
		InputStream stream = bufferToStream(buf, entry.getDataStart(), (int) entry.getCompressedSize());
		return new OwnedInflaterInputStream(stream, new Inflater(true), BUFFER_SIZE);
	}

	/**
	 * {@link InflaterInputStream} releases the inflater native memory on close only if it created
	 * the inflater itself. Since a custom inflater is used here, end it explicitly.
	 */
	private static final class OwnedInflaterInputStream extends InflaterInputStream {
		private boolean closed;

		OwnedInflaterInputStream(InputStream in, Inflater inflater, int size) {
			super(in, inflater, size);
		}

		@Override
		public void close() throws IOException {
			if (closed) {
				return;
			}
			closed = true;
			try {
				super.close();
			} finally {
				inf.end();
			}
		}
	}
}
