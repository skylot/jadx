package jadx.gui.utils.elf;

import org.exbin.auxiliary.binary_data.BinaryData;

import net.fornwall.jelf.BackingFile;

/**
 * Adapter class for using a hexview BinaryData as a BackingFile, ready for JElf to read.
 */
public class BinaryDataBackingFile implements BackingFile {

	private final BinaryData backing;
	private long pos;

	public BinaryDataBackingFile(BinaryData backing) {
		this.backing = backing;
		this.pos = 0;
	}

	@Override
	public void seek(long offset) {
		pos = offset;
	}

	@Override
	public void skip(int bytesToSkip) {
		pos += bytesToSkip;
	}

	@Override
	public short readUnsignedByte() {
		// despite returning short, this really should only read a byte
		long oldPos = pos;
		pos += 1;
		return this.backing.getByte(oldPos);
	}

	@Override
	public int read(byte[] data) {
		for (int i = 0; i < data.length; i++) {
			data[i] = (byte) readUnsignedByte();
		}
		return data.length;
	}

}
