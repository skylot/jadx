package jadx.zip.parser;

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
		byte[] out = new byte[(int) entry.getUncompressedSize()];
		Inflater inflater = new Inflater(true);
		inflater.setInput(entryBuf);
		int written = inflater.inflate(out);
		inflater.end();
		if (written != out.length) {
			throw new DataFormatException("Unexpected size of decompressed entry: " + entry
					+ ", got: " + written + ", expected: " + out.length);
		}
		return out;
	}

	static InputStream decompressEntryToStream(ByteBuffer buf, JadxZipEntry entry) {
		InputStream stream = bufferToStream(buf, entry.getDataStart(), (int) entry.getCompressedSize());
		Inflater inflater = new Inflater(true);
		return new InflaterInputStream(stream, inflater, BUFFER_SIZE);
	}
}
