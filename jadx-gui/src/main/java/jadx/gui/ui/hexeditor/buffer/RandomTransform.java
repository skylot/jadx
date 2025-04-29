package jadx.gui.ui.hexeditor.buffer;

import java.util.Random;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public class RandomTransform extends ByteTransform {
	public static final RandomTransform RANDOM = new RandomTransform();

	private final Random random;

	private RandomTransform() {
		super("Random Fill");
		this.random = new Random();
	}

	@Override
	public boolean transform(byte[] data, int offset, int length) {
		if (length > 0) {
			byte[] newData = new byte[length];
			random.nextBytes(newData);
			for (int i = 0; i < length; i++) {
				data[offset] = newData[i];
				offset++;
			}
			return true;
		}
		return false;
	}
}
