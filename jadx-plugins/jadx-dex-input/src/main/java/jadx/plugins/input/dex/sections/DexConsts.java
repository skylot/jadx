package jadx.plugins.input.dex.sections;

public class DexConsts {

	public static final byte[] DEX_FILE_MAGIC = { 0x64, 0x65, 0x78, 0x0a }; // 'dex\n'

	public static final byte[] ZIP_FILE_MAGIC = { 0x50, 0x4B, 0x03, 0x04 };

	public static final int MAX_MAGIC_SIZE = 4;

	public static final int ENDIAN_CONSTANT = 0x12345678;

	public static final int NO_INDEX = -1;
}
