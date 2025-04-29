package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class ReverseTransform extends ByteTransform {
	public static final ReverseTransform BYTES = new ReverseTransform("Reverse Bytes", true, false, false);
	public static final ReverseTransform NYBBLES = new ReverseTransform("Reverse Nybbles", true, true, false);
	public static final ReverseTransform BITS = new ReverseTransform("Reverse Bits", true, false, true);

	private final boolean reverseBytes;
	private final boolean reverseNybbles;
	private final boolean reverseBits;

	public ReverseTransform(
			String name,
			boolean reverseBytes,
			boolean reverseNybbles,
			boolean reverseBits) {
		super(name);
		this.reverseBytes = reverseBytes;
		this.reverseNybbles = reverseNybbles;
		this.reverseBits = reverseBits;
	}

	@Override
	public boolean transform(byte[] data, int offset, int length) {
		if (length > 0) {
			if (reverseBytes) {
				for (int i = 0, n = length / 2; i < n; i++) {
					byte tmp = data[offset + i];
					data[offset + i] = data[offset + length - i - 1];
					data[offset + length - i - 1] = tmp;
				}
			}
			if (reverseNybbles) {
				for (int i = 0; i < length; i++) {
					byte tmp = data[offset + i];
					tmp = (byte) (((tmp >> 4) & 0x0F) | (tmp << 4));
					data[offset + i] = tmp;
				}
			}
			if (reverseBits) {
				for (int i = 0; i < length; i++) {
					byte tmp = data[offset + i];
					tmp = (byte) (Integer.reverse(tmp) >> 24);
					data[offset + i] = tmp;
				}
			}
			return true;
		}
		return false;
	}
}
