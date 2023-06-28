package jadx.core.xmlgen;

import java.io.IOException;

public class CommonBinaryParser extends ParserConstants {
	protected ParserStream is;

	protected BinaryXMLStrings parseStringPool() throws IOException {
		is.checkInt16(RES_STRING_POOL_TYPE, "String pool expected");
		return parseStringPoolNoType();
	}

	protected BinaryXMLStrings parseStringPoolNoType() throws IOException {
		long start = is.getPos() - 2;
		is.checkInt16(0x001c, "String pool header size not 0x001c");
		long size = is.readUInt32();
		long chunkEnd = start + size;

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
