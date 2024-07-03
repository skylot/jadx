package jadx.core.xmlgen;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BinaryXMLStrings {
	public static final String INVALID_STRING_PLACEHOLDER = "⟨STRING_DECODE_ERROR⟩";
	private final int stringCount;

	private final long stringsStart;

	private final ByteBuffer buffer;

	private final boolean isUtf8;

	// This cache include strings that have been overridden by the deobfuscator.
	private final Map<Integer, String> cache = new HashMap<>();

	public BinaryXMLStrings() {
		stringCount = 0;
		stringsStart = 0;
		buffer = ByteBuffer.allocate(0);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		isUtf8 = false;
	}

	public BinaryXMLStrings(int stringCount, long stringsStart, byte[] buffer, boolean isUtf8) {
		this.stringCount = stringCount;
		this.stringsStart = stringsStart;
		this.buffer = ByteBuffer.wrap(buffer);
		this.buffer.order(ByteOrder.LITTLE_ENDIAN);
		this.isUtf8 = isUtf8;
	}

	public String get(int id) {
		String cached = cache.get(id);
		if (cached != null) {
			return cached;
		}

		if (id * 4 >= buffer.limit() - 3) {
			return INVALID_STRING_PLACEHOLDER;
		}

		long offset = stringsStart + buffer.getInt(id * 4);
		String extracted;
		if (isUtf8) {
			extracted = extractString8(this.buffer.array(), (int) offset);
		} else {
			// don't trust specified string length, read until \0
			// stringsOffset can be same for different indexes
			extracted = extractString16(this.buffer.array(), (int) offset);
		}
		cache.put(id, extracted);
		return extracted;
	}

	public void put(int id, String content) {
		cache.put(id, content);
	}

	public int size() {
		return this.stringCount;
	}

	private static String extractString8(byte[] strArray, int offset) {
		if (offset >= strArray.length) {
			return INVALID_STRING_PLACEHOLDER;
		}
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
		if (offset + 2 >= strArray.length) {
			return INVALID_STRING_PLACEHOLDER;
		}

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
}
