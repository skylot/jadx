package jadx.gui.ui.hexeditor.buffer;

/*
 * code taken from https://github.com/kreativekorp/hexcellent
 */
public abstract class ByteTransform {
	protected final String name;

	protected ByteTransform(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	@Override
	public final String toString() {
		return this.name;
	}

	public abstract boolean transform(byte[] data, int offset, int length);
}
