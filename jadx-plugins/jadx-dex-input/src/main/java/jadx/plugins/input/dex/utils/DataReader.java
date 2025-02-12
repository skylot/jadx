package jadx.plugins.input.dex.utils;

public class DataReader {

	public static int readU4(byte[] data, int pos) {
		byte b1 = data[pos++];
		byte b2 = data[pos++];
		byte b3 = data[pos++];
		byte b4 = data[pos];
		return (b4 & 0xFF) << 24 | (b3 & 0xFF) << 16 | (b2 & 0xFF) << 8 | b1 & 0xFF;
	}
}
