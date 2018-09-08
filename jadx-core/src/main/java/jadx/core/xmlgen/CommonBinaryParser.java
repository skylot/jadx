package jadx.core.xmlgen;

import java.io.IOException;
import java.util.Arrays;

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
		long chunkEnd = start + size;

		int stringCount = is.readInt32();
		int styleCount = is.readInt32();
		int flags = is.readInt32();
		long stringsStart = is.readInt32();
		long stylesStart = is.readInt32();

		int[] stringsOffset = is.readInt32Array(stringCount);
		int[] stylesOffset = is.readInt32Array(styleCount);

		is.checkPos(start + stringsStart, "Expected strings start");
		long stringsEnd = stylesStart == 0 ? chunkEnd : start + stylesStart;
		String[] strings = new String[stringCount];
		byte[] strArray = is.readInt8Array((int) (stringsEnd - is.getPos()));
		if ((flags & UTF8_FLAG) != 0) {
			// UTF-8
			for (int i = 0; i < stringCount; i++) {
				strings[i] = extractString8(strArray, stringsOffset[i]);
			}
		} else {
			// UTF-16
			for (int i = 0; i < stringCount; i++) {
				// don't trust specified string length, read until \0
				// stringsOffset can be same for different indexes
				strings[i] = extractString16(strArray, stringsOffset[i]);
			}
		}
		if (stylesStart != 0) {
			is.checkPos(start + stylesStart, "Expected styles start");
			if (styleCount != 0) {
				// TODO: implement styles parsing
			}
		}
		// skip padding zeroes
		is.skipToPos(chunkEnd, "Skip string pool padding");
		return strings;
	}

	private static String extractString8(byte[] strArray, int offset) {
		int start = offset + skipStrLen8(strArray, offset);
		int len = strArray[start++];
		if (len == 0) {
			return "";
		}
		if ((len & 0x80) != 0) {
			len = (len & 0x7F) << 8 | strArray[start++] & 0xFF;
		}
		byte[] arr = Arrays.copyOfRange(strArray, start, start + len);
		return new String(arr, ParserStream.STRING_CHARSET_UTF8);
	}

	private static String extractString16(byte[] strArray, int offset) {
		int len = strArray.length;
		int start = offset + skipStrLen16(strArray, offset);
		int end = start;
		while (true) {
			if (end + 1 >= len) {
				break;
			}
			if (strArray[end] == 0 && strArray[end + 1] == 0) {
				break;
			}
			end += 2;
		}
		byte[] arr = Arrays.copyOfRange(strArray, start, end);
		return new String(arr, ParserStream.STRING_CHARSET_UTF16);
	}

	private static int skipStrLen8(byte[] strArray, int offset) {
		return (strArray[offset] & 0x80) == 0 ? 1 : 2;
	}

	private static int skipStrLen16(byte[] strArray, int offset) {
		return (strArray[offset + 1] & 0x80) == 0 ? 2 : 4;
	}

	protected void die(String message) throws IOException {
		throw new IOException("Decode error: " + message
				+ ", position: 0x" + Long.toHexString(is.getPos()));
	}
}
