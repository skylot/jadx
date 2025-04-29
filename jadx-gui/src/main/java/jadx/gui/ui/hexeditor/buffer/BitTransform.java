package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class BitTransform extends ByteTransform {
	public static final BitTransform ZERO = new BitTransform("Zero Fill", BitOperation.FILL, (byte) 0);
	public static final BitTransform ONE = new BitTransform("One Fill", BitOperation.FILL, (byte) (-1));
	public static final BitTransform SPACE = new BitTransform("Space Fill", BitOperation.FILL, (byte) 0x20);
	public static final BitTransform INVERT = new BitTransform("Invert", BitOperation.XOR, (byte) (-1));
	public static final BitTransform INVERT_MSB = new BitTransform("Invert MSB", BitOperation.XOR, (byte) 0x80);

	private final BitOperation op;
	private final byte[] mask;

	public BitTransform(String name, BitOperation op, byte... mask) {
		super(name);
		this.op = op;
		this.mask = mask;
	}

	@Override
	public boolean transform(byte[] data, int offset, int length) {
		if (length > 0) {
			for (int i = 0; i < length; i++) {
				data[offset] = op.op(data[offset], mask[i % mask.length]);
				offset++;
			}
			return true;
		}
		return false;
	}
}
