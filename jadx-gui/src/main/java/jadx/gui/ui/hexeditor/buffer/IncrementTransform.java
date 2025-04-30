package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class IncrementTransform extends ByteTransform {
	public static final IncrementTransform INC_LE = new IncrementTransform("Increment", true, true);
	public static final IncrementTransform INC_BE = new IncrementTransform("Increment", true, false);
	public static final IncrementTransform DEC_LE = new IncrementTransform("Decrement", false, true);
	public static final IncrementTransform DEC_BE = new IncrementTransform("Decrement", false, false);

	private final boolean increment;
	private final boolean le;

	private IncrementTransform(String name, boolean increment, boolean le) {
		super(name);
		this.increment = increment;
		this.le = le;
	}

	@Override
	public boolean transform(byte[] data, int offset, int length) {
		if (length > 0) {
			if (increment) {
				if (le) {
					for (int i = 0; i < length; i++) {
						if ((++data[offset + i]) != 0) {
							break;
						}
					}
				} else {
					for (int i = length - 1; i >= 0; i--) {
						if ((++data[offset + i]) != 0) {
							break;
						}
					}
				}
			} else {
				if (le) {
					for (int i = 0; i < length; i++) {
						if ((data[offset + i]--) != 0) {
							break;
						}
					}
				} else {
					for (int i = length - 1; i >= 0; i--) {
						if ((data[offset + i]--) != 0) {
							break;
						}
					}
				}
			}
			return true;
		}
		return false;
	}
}
