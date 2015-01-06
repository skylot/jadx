package jadx.core.xmlgen;

import java.io.IOException;

public class CommonBinaryParser extends ParserConstants {

	protected ParserStream is;

	protected String[] parseStringPool() throws IOException {
		is.checkInt16(RES_STRING_POOL_TYPE, "String pool expected");
		return parseStringPoolNoType();
	}

	protected String[] parseStringPoolNoType() throws IOException {
		long start = is.getPos() - 2;
		is.checkInt16(0x001c, "String pool header size not 0x001c");
		long size = is.readUInt32();

		int stringCount = is.readInt32();
		int styleCount = is.readInt32();
		int flags = is.readInt32();
		long stringsStart = is.readInt32();
		long stylesStart = is.readInt32();

		int[] stringsOffset = is.readInt32Array(stringCount);
		int[] stylesOffset = is.readInt32Array(styleCount);

		is.checkPos(start + stringsStart, "Expected strings start");
		String[] strings = new String[stringCount];
		if ((flags & UTF8_FLAG) != 0) {
			// UTF-8
			for (int i = 0; i < stringCount; i++) {
				// is.checkPos(start + stringsStart + stringsOffset[i], "Expected string start");
				strings[i] = is.readString8();
			}
		} else {
			// UTF-16
			long stringsStartOffset = start + stringsStart;
			for (int i = 0; i < stringCount; i++) {
				// is.checkPos(stringsStartOffset + stringsOffset[i], "Expected string start");
				// TODO: don't trust specified string length, read until \0
				// TODO: stringsOffset can be same for different indexes
				strings[i] = is.readString16();
			}
		}
		if (stylesStart != 0) {
			is.checkPos(start + stylesStart, "Expected styles start");
			if (styleCount != 0) {
				// TODO: implement styles parsing
			}
		}
		// skip padding zeroes
		is.skip(start + size - is.getPos());
		return strings;
	}

	protected void die(String message) throws IOException {
		throw new IOException("Decode error: " + message
				+ ", position: 0x" + Long.toHexString(is.getPos()));
	}

}
