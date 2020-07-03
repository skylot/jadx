package jadx.plugins.input.dex.utils;

import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.sections.SectionReader;

public class MUtf8 {

	public static String decode(SectionReader in) {
		int len = in.readUleb128();
		char[] out = new char[len];
		int k = 0;
		while (true) {
			char a = (char) (in.readUByte() & 0xff);
			if (a == 0) {
				return new String(out, 0, k);
			}
			out[k] = a;
			if (a < '\u0080') {
				k++;
			} else if ((a & 0xE0) == 0xC0) {
				int b = in.readUByte();
				if ((b & 0xC0) != 0x80) {
					throw new DexException("Bad second byte");
				}
				out[k] = (char) (((a & 0x1F) << 6) | (b & 0x3F));
				k++;
			} else if ((a & 0xF0) == 0xE0) {
				int b = in.readUByte();
				int c = in.readUByte();
				if (((b & 0xC0) != 0x80) || ((c & 0xC0) != 0x80)) {
					throw new DexException("Bad second or third byte");
				}
				out[k] = (char) (((a & 0x0F) << 12) | ((b & 0x3F) << 6) | (c & 0x3F));
				k++;
			} else {
				throw new DexException("Bad byte");
			}
		}
	}
}
