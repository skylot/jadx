package jadx.plugins.input.java.data.attributes.stack;

import jadx.plugins.input.java.data.DataReader;

public class TypeInfoReader {
	private static final int ITEM_TOP = 0;
	private static final int ITEM_INT = 1;
	private static final int ITEM_FLOAT = 2;

	private static final int ITEM_DOUBLE = 3;
	private static final int ITEM_LONG = 4;

	private static final int ITEM_NULL = 5;
	private static final int ITEM_UNINITIALIZED_THIS = 6;

	private static final int ITEM_OBJECT = 7;
	private static final int ITEM_UNINITIALIZED = 8;

	static StackValueType[] readTypeInfoList(DataReader reader, int count) {
		StackValueType[] types = new StackValueType[count];
		for (int i = 0; i < count; i++) {
			int tag = reader.readU1();
			StackValueType type;
			switch (tag) {
				case ITEM_DOUBLE:
				case ITEM_LONG:
					type = StackValueType.WIDE;
					break;

				case ITEM_OBJECT:
				case ITEM_UNINITIALIZED:
					reader.readU2(); // ignore
					type = StackValueType.NARROW;
					break;

				default:
					type = StackValueType.NARROW;
					break;
			}
			types[i] = type;
		}
		return types;
	}

	static void skipTypeInfoList(DataReader reader, int count) {
		for (int i = 0; i < count; i++) {
			int tag = reader.readU1();
			if (tag == ITEM_OBJECT || tag == ITEM_UNINITIALIZED) {
				reader.readU2();
			}
		}
	}
}
