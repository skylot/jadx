package jadx.plugins.input.dex.utils;

import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.sections.SectionReader;

public final class Leb128 {

	public static int readSignedLeb128(SectionReader in) {
		int result = 0;
		int cur;
		int count = 0;
		int signBits = -1;
		do {
			cur = in.readUByte();
			result |= (cur & 0x7f) << (count * 7);
			signBits <<= 7;
			count++;
		} while (((cur & 0x80) == 0x80) && count < 5);

		if ((cur & 0x80) == 0x80) {
			throw new DexException("Invalid LEB128 sequence");
		}
		// Sign extend if appropriate
		if (((signBits >> 1) & result) != 0) {
			result |= signBits;
		}
		return result;
	}

	public static int readUnsignedLeb128(SectionReader in) {
		int result = 0;
		int cur;
		int count = 0;
		do {
			cur = in.readUByte();
			result |= (cur & 0x7f) << (count * 7);
			count++;
		} while (((cur & 0x80) == 0x80) && count < 5);

		if ((cur & 0x80) == 0x80) {
			throw new DexException("Invalid LEB128 sequence");
		}
		return result;
	}
}
