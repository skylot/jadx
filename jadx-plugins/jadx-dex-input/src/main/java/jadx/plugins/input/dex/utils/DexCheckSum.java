package jadx.plugins.input.dex.utils;

import java.util.zip.Adler32;

import jadx.plugins.input.dex.DexException;

public class DexCheckSum {

	public static void verify(String fileName, byte[] content, int offset) {
		if (offset + 32 + 4 > content.length) {
			throw new DexException("Dex file truncated, can't read file length, file: " + fileName);
		}
		int len = DataReader.readU4(content, offset + 32);
		if (offset + len > content.length) {
			throw new DexException("Dex file truncated, length in header: " + len + ", file: " + fileName);
		}
		int checksum = DataReader.readU4(content, offset + 8);
		Adler32 adler32 = new Adler32();
		adler32.update(content, offset + 12, len - 12);
		int fileChecksum = (int) adler32.getValue();
		if (checksum != fileChecksum) {
			throw new DexException(String.format("Bad dex file checksum: 0x%08x, expected: 0x%08x, file: %s",
					fileChecksum, checksum, fileName));
		}
	}
}
