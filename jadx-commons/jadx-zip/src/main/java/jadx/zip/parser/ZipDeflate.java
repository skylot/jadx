package jadx.zip.parser;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class ZipDeflate {

	static byte[] decompressEntryToBytes(ByteBuffer buf, JadxZipEntry entry) throws DataFormatException {
		buf.position(entry.getDataStart());
		ByteBuffer entryBuf = buf.slice();
		entryBuf.limit((int) entry.getCompressedSize());
		byte[] out = new byte[(int) entry.getUncompressedSize()];
		Inflater inflater = new Inflater(true);
		inflater.setInput(entryBuf);
		int written = inflater.inflate(out);
		if (written != out.length) {
			throw new DataFormatException("Unexpected size of decompressed entry: " + entry
					+ ", got: " + written + ", expected: " + out.length);
		}
		inflater.end();
		return out;
	}
}
