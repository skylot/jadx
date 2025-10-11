package jadx.plugins.input.java.utils;

import java.nio.charset.StandardCharsets;

public class ModifiedUTF8Decoder {

	public static String decodeString(byte[] bytes) {
		int len = bytes.length;
		// quick check if all chars are 7-bit
		boolean asciiStr = true;
		for (byte b : bytes) {
			if ((b & 0x80) != 0) {
				asciiStr = false;
				break;
			}
		}
		if (asciiStr) {
			return new String(bytes, StandardCharsets.US_ASCII);
		}

		// parse modified UTF-8 according jvms-4.4.7
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < len; i++) {
			int x = bytes[i] & 0xff;
			// 4.4 ascii characters 1-127 (0 is encoded as 0xc0 0x80)
			if ((x & 0x80) == 0) {
				// 1 byte 7-Bit ascii (Table 4.4./4.5)
				sb.append((char) x);
			} else {
				if (i + 1 >= len) {
					throw new JavaClassParseException("Inconsistent byte array structure: too short");
				}
				int y = bytes[i + 1] & 0xff;
				// 0 is encoded as 0xc0 0x80 (jvms-4.4.7)
				if (x == 0xc0 && y == 0x80) {
					sb.appendCodePoint(0);
					i++;
				} else if ((x & 0xE0) == 0xC0 && (y & 0xC0) == 0x80) {
					// 2 byte char (Table 4.8./4.9 )
					sb.appendCodePoint(((x & 0x1f) << 6) + (y & 0x3f));
					i++;
				} else if (i + 2 < len) {
					int z = bytes[i + 2] & 0xff;
					if ((x & 0xF0) == 0xE0 && (y & 0xC0) == 0x80 && (z & 0xC0) == 0x80) {
						// 3 byte char (Table 4.11/4.12)
						sb.appendCodePoint(((x & 0xf) << 12) + ((y & 0x3f) << 6) + (z & 0x3f));
						i += 2;
					} else if (i + 5 < len
							&& x == 0xED // u
							&& (y & 0xF0) == 0xA0 // v
							&& (bytes[i + 3] & 0xff) == 0xED // x
							&& (bytes[i + 4] & 0xF0) == 0xA0 // y
					) {
						// 6 byte encoded Table 4.12.
						int u = x; // 0
						int v = y; // 1
						int w = z; // 2
						x = bytes[i + 3] & 0xff;
						y = bytes[i + 4] & 0xff;
						z = bytes[i + 5] & 0xff;
						if (x == 0xED && (y & 0xF0) == 0xA0) {
							sb.appendCodePoint(0x10000 + ((v & 0x0f) << 16) + ((w & 0x3f) << 10) + ((y & 0x0f) << 6) + (z & 0x3f));
							i += 5;
						} else {
							throw new JavaClassParseException("Inconsistent byte array structure: invalid 6 bytes char");
						}
					} else {
						throw new JavaClassParseException("Inconsistent byte array structure: unexpected char");
					}
				}
			}
		}
		return sb.toString();
	}
}
