package jadx.core.utils.zip;

import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

final class ZipDeflate {

	static byte[] decompressEntryToBytes(ByteBuffer buf, ZipFileEntry entry) throws DataFormatException {
		byte[] in = new byte[entry.getCompressedSize()];
		buf.position(entry.getDataStart());
		buf.get(in);
		byte[] out = new byte[entry.getUncompressedSize()];
		Inflater inflater = new Inflater(true);
		inflater.setInput(in);
		inflater.inflate(out);
		inflater.end();
		return out;
	}
}
