package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class RotateTransform extends ByteTransform {
	public static final RotateTransform ROL_LE = new RotateTransform("Rotate Left", true, true, false, true);
	public static final RotateTransform ROL_BE = new RotateTransform("Rotate Left", true, true, false, false);
	public static final RotateTransform ROR_LE = new RotateTransform("Rotate Right", true, false, false, true);
	public static final RotateTransform ROR_BE = new RotateTransform("Rotate Right", true, false, false, false);
	public static final RotateTransform ASL_LE = new RotateTransform("Shift Left", false, true, false, true);
	public static final RotateTransform ASL_BE = new RotateTransform("Shift Left", false, true, false, false);
	public static final RotateTransform LSR_LE = new RotateTransform("Shift Right", false, false, false, true);
	public static final RotateTransform LSR_BE = new RotateTransform("Shift Right", false, false, false, false);
	public static final RotateTransform ASR_LE = new RotateTransform("Shift Right", false, false, true, true);
	public static final RotateTransform ASR_BE = new RotateTransform("Shift Right", false, false, true, false);

	private final boolean rotate;
	private final boolean left;
	private final boolean extend;
	private final boolean le;

	private RotateTransform(String name, boolean rotate, boolean left, boolean extend, boolean le) {
		super(name);
		this.rotate = rotate;
		this.left = left;
		this.extend = extend;
		this.le = le;
	}

	@Override
	public boolean transform(byte[] data, int offset, int length) {
		if (length > 0) {
			if (left) {
				if (le) {
					int f = rotate ? ((data[offset + length - 1] >> 7) & 1) : 0;
					for (int i = length - 1; i > 0; i--) {
						data[offset + i] <<= 1;
						data[offset + i] |= ((data[offset + i - 1] >> 7) & 1);
					}
					data[offset] <<= 1;
					data[offset] |= f;
				} else {
					int f = rotate ? ((data[offset] >> 7) & 1) : 0;
					for (int i = 0; i < length - 1; i++) {
						data[offset + i] <<= 1;
						data[offset + i] |= ((data[offset + i + 1] >> 7) & 1);
					}
					data[offset + length - 1] <<= 1;
					data[offset + length - 1] |= f;
				}
			} else {
				if (le) {
					int f = rotate ? ((data[offset] << 7) & 0x80) : extend ? (data[offset + length - 1] & 0x80) : 0;
					for (int i = 0; i < length - 1; i++) {
						data[offset + i] >>= 1;
						data[offset + i] &= 0x7F;
						data[offset + i] |= ((data[offset + i + 1] << 7) & 0x80);
					}
					data[offset + length - 1] >>= 1;
					data[offset + length - 1] &= 0x7F;
					data[offset + length - 1] |= f;
				} else {
					int f = rotate ? ((data[offset + length - 1] << 7) & 0x80) : extend ? (data[offset] & 0x80) : 0;
					for (int i = length - 1; i > 0; i--) {
						data[offset + i] >>= 1;
						data[offset + i] &= 0x7F;
						data[offset + i] |= ((data[offset + i - 1] << 7) & 0x80);
					}
					data[offset] >>= 1;
					data[offset] &= 0x7F;
					data[offset] |= f;
				}
			}
			return true;
		}
		return false;
	}
}
