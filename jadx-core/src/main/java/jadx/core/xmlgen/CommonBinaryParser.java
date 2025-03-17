package jadx.core.xmlgen;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonBinaryParser extends ParserConstants {
	private static final Logger LOG = LoggerFactory.getLogger(CommonBinaryParser.class);

	protected ParserStream is;

	protected BinaryXMLStrings parseStringPool() throws IOException {
		is.checkInt16(RES_STRING_POOL_TYPE, "String pool expected");
		return parseStringPoolNoType();
	}

	protected BinaryXMLStrings parseStringPoolNoType() throws IOException {
		long start = is.getPos() - 2;
		int headerSize = is.readInt16();
		if (headerSize != 0x1c) {
			LOG.warn("Unexpected string pool header size: 0x{}, expected: 0x1C", Integer.toHexString(headerSize));
		}
		long size = is.readUInt32();
		long chunkEnd = start + size;

		return parseStringPoolNoSize(start, chunkEnd);
	}

	protected BinaryXMLStrings parseStringPoolNoSize(long start, long chunkEnd) throws IOException {
		int stringCount = is.readInt32();
		int styleCount = is.readInt32();
		int flags = is.readInt32();
		long stringsStart = is.readInt32();
		long stylesStart = is.readInt32();

		// Correct the offset of actual strings, as the header is already read.
		stringsStart = stringsStart - (is.getPos() - start);
		byte[] buffer = is.readInt8Array((int) (chunkEnd - is.getPos()));
		is.checkPos(chunkEnd, "Expected strings pool end");

		return new BinaryXMLStrings(
				stringCount,
				stringsStart,
				buffer,
				(flags & UTF8_FLAG) != 0);
	}

	protected void die(String message) throws IOException {
		throw new IOException("Decode error: " + message
				+ ", position: 0x" + Long.toHexString(is.getPos()));
	}
}
