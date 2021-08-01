package jadx.plugins.input.java.data;

import java.util.stream.Stream;

import jadx.plugins.input.java.utils.JavaClassParseException;

public enum ConstantType {
	UTF8(1, -1),
	INTEGER(3, 4),
	FLOAT(4, 4),
	LONG(5, 8),
	DOUBLE(6, 8),
	CLASS(7, 2),
	STRING(8, 2),
	FIELD_REF(9, 4),
	METHOD_REF(10, 4),
	INTERFACE_METHOD_REF(11, 4),
	NAME_AND_TYPE(12, 4),
	METHOD_HANDLE(15, 3),
	METHOD_TYPE(16, 2),
	DYNAMIC(17, 4),
	INVOKE_DYNAMIC(18, 4),
	MODULE(19, 2),
	PACKAGE(20, 2);

	private static final ConstantType[] TAG_MAP;

	static {
		ConstantType[] values = ConstantType.values();
		int maxVal = Stream.of(values)
				.mapToInt(ConstantType::getTag)
				.max()
				.orElseThrow(() -> new IllegalArgumentException("Empty ConstantType enum"));

		ConstantType[] map = new ConstantType[maxVal + 1];
		for (ConstantType value : values) {
			map[value.getTag()] = value;
		}
		TAG_MAP = map;
	}

	public static ConstantType getTypeByTag(int tag) {
		ConstantType type = TAG_MAP[tag];
		if (type == null) {
			throw new JavaClassParseException("Unknown constant pool tag: " + tag);
		}
		return type;
	}

	private final byte tag;
	private final int dataSize;

	ConstantType(int tag, int dataSize) {
		this.tag = (byte) tag;
		this.dataSize = dataSize;
	}

	public byte getTag() {
		return tag;
	}

	public int getDataSize() {
		return dataSize;
	}
}
