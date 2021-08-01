package jadx.plugins.input.java.data;

public class ClassOffsets {

	private final int[] constPoolOffsets;
	private final int constPoolEnd;
	private final int interfacesEnd;
	private final int attributesOffset;

	public ClassOffsets(DataReader data) {
		this.constPoolOffsets = readConstPool(data);
		this.constPoolEnd = data.getOffset();
		int interfacesCount = data.absPos(constPoolEnd + 6).readU2();
		data.skip(interfacesCount * 2);
		this.interfacesEnd = data.getOffset();
		skipFields(data);
		skipMethods(data);
		this.attributesOffset = data.getOffset();
	}

	private static int[] readConstPool(DataReader data) {
		int cpSize = data.absPos(8).readU2();
		int[] cpOffsets = new int[cpSize + 1];
		for (int i = 1; i < cpSize; i++) {
			int tag = data.readU1();
			cpOffsets[i] = data.getOffset();
			ConstantType constType = ConstantType.getTypeByTag(tag);
			switch (constType) {
				case UTF8:
					data.skip(data.readU2());
					break;

				case LONG:
				case DOUBLE:
					data.skip(8);
					i++;
					break;

				default:
					data.skip(constType.getDataSize());
					break;
			}
		}
		return cpOffsets;
	}

	private void skipFields(DataReader data) {
		int fieldsCount = data.readU2();
		for (int i = 0; i < fieldsCount; i++) {
			data.skip(6);
			skipAttributes(data);
		}
	}

	private void skipMethods(DataReader data) {
		int methodsCount = data.readU2();
		for (int i = 0; i < methodsCount; i++) {
			data.skip(6);
			skipAttributes(data);
		}
	}

	private void skipAttributes(DataReader data) {
		int attrCount = data.readU2();
		for (int i = 0; i < attrCount; i++) {
			data.skip(2);
			int len = data.readU4();
			data.skip(len);
		}
	}

	public int getOffsetOfConstEntry(int num) {
		return constPoolOffsets[num];
	}

	public int getAccessFlagsOffset() {
		return constPoolEnd;
	}

	public int getClsTypeOffset() {
		return constPoolEnd + 2;
	}

	public int getSuperTypeOffset() {
		return constPoolEnd + 4;
	}

	public int getInterfacesOffset() {
		return constPoolEnd + 6;
	}

	public int getFieldsOffset() {
		return interfacesEnd;
	}

	public int getAttributesOffset() {
		return attributesOffset;
	}
}
