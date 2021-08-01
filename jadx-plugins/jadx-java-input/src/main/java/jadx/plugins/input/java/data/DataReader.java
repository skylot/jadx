package jadx.plugins.input.java.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DataReader {
	private final byte[] data;
	private int offset;

	public DataReader(byte[] data) {
		this(data, 0);
	}

	public DataReader(byte[] data, int offset) {
		this.data = data;
		this.offset = offset;
	}

	public DataReader copy() {
		return new DataReader(data, offset);
	}

	public DataReader absPos(int offset) {
		this.offset = offset;
		return this;
	}

	public int getOffset() {
		return offset;
	}

	public void skip(int size) {
		this.offset += size;
	}

	public int readS1() {
		int pos = this.offset;
		byte b1 = this.data[pos];
		this.offset = pos + 1;
		return b1;
	}

	public int readU1() {
		int pos = this.offset;
		byte b1 = this.data[pos];
		this.offset = pos + 1;
		return b1 & 0xFF;
	}

	public int readS2() {
		int pos = this.offset;
		byte[] d = this.data;
		byte b1 = d[pos++];
		byte b2 = d[pos++];
		this.offset = pos;
		return b1 << 8 | (b2 & 0xFF);
	}

	public int readU2() {
		int pos = this.offset;
		byte[] d = this.data;
		byte b1 = d[pos++];
		byte b2 = d[pos++];
		this.offset = pos;
		return (b1 & 0xFF) << 8 | (b2 & 0xFF);
	}

	public int readS4() {
		int pos = this.offset;
		byte[] d = this.data;
		byte b1 = d[pos++];
		byte b2 = d[pos++];
		byte b3 = d[pos++];
		byte b4 = d[pos++];
		this.offset = pos;
		return b1 << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
	}

	public int readU4() {
		int pos = this.offset;
		byte[] d = this.data;
		byte b1 = d[pos++];
		byte b2 = d[pos++];
		byte b3 = d[pos++];
		byte b4 = d[pos++];
		this.offset = pos;
		return (b1 & 0xFF) << 24 | (b2 & 0xFF) << 16 | (b3 & 0xFF) << 8 | (b4 & 0xFF);
	}

	public long readS8() {
		long high = readS4();
		long low = readU4() & 0xFFFF_FFFFL;
		return high << 32 | low;
	}

	public long readU8() {
		long high = readU4() & 0xFFFF_FFFFL;
		long low = readU4() & 0xFFFF_FFFFL;
		return high << 32 | low;
	}

	public byte[] readBytes(int len) {
		int pos = this.offset;
		this.offset = pos + len;
		return Arrays.copyOfRange(data, pos, pos + len);
	}

	public List<String> readClassesList(ConstPoolReader constPool) {
		int len = readU2();
		if (len == 0) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>(len);
		for (int i = 0; i < len; i++) {
			list.add(constPool.getClass(readU2()));
		}
		return list;
	}

	public byte[] getBytes() {
		return data;
	}
}
