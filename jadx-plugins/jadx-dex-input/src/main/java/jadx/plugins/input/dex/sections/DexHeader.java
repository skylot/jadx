package jadx.plugins.input.dex.sections;

import jadx.plugins.input.dex.DexException;

public class DexHeader {
	private final String version;
	private final int classDefsSize;
	private final int classDefsOff;
	private final int stringIdsOff;
	private final int typeIdsOff;
	private final int typeIdsSize;
	private final int fieldIdsSize;
	private final int fieldIdsOff;
	private final int protoIdsSize;
	private final int protoIdsOff;
	private final int methodIdsOff;
	private final int methodIdsSize;

	private int callSiteOff;
	private int methodHandleOff;

	public DexHeader(SectionReader buf) {
		byte[] magic = buf.readByteArray(4);
		version = buf.readString(3);
		buf.skip(1);
		int checksum = buf.readInt();
		byte[] signature = buf.readByteArray(20);
		int fileSize = buf.readInt();
		int headerSize = buf.readInt();
		int endianTag = buf.readInt();
		if (endianTag != DexConsts.ENDIAN_CONSTANT) {
			throw new DexException("Unexpected endian tag: 0x" + Integer.toHexString(endianTag));
		}
		int linkSize = buf.readInt();
		int linkOff = buf.readInt();
		int mapListOff = buf.readInt();
		int stringIdsSize = buf.readInt();
		stringIdsOff = buf.readInt();
		typeIdsSize = buf.readInt();
		typeIdsOff = buf.readInt();
		protoIdsSize = buf.readInt();
		protoIdsOff = buf.readInt();
		fieldIdsSize = buf.readInt();
		fieldIdsOff = buf.readInt();
		methodIdsSize = buf.readInt();
		methodIdsOff = buf.readInt();
		classDefsSize = buf.readInt();
		classDefsOff = buf.readInt();
		int dataSize = buf.readInt();
		int dataOff = buf.readInt();

		readMapList(buf, mapListOff);
	}

	private void readMapList(SectionReader buf, int mapListOff) {
		buf.absPos(mapListOff);
		int size = buf.readInt();
		for (int i = 0; i < size; i++) {
			int type = buf.readUShort();
			buf.skip(6);
			int offset = buf.readInt();

			switch (type) {
				case 0x0007:
					callSiteOff = offset;
					break;

				case 0x0008:
					methodHandleOff = offset;
					break;
			}
		}
	}

	public String getVersion() {
		return version;
	}

	public int getClassDefsSize() {
		return classDefsSize;
	}

	public int getClassDefsOff() {
		return classDefsOff;
	}

	public int getStringIdsOff() {
		return stringIdsOff;
	}

	public int getTypeIdsOff() {
		return typeIdsOff;
	}

	public int getTypeIdsSize() {
		return typeIdsSize;
	}

	public int getFieldIdsSize() {
		return fieldIdsSize;
	}

	public int getFieldIdsOff() {
		return fieldIdsOff;
	}

	public int getProtoIdsSize() {
		return protoIdsSize;
	}

	public int getProtoIdsOff() {
		return protoIdsOff;
	}

	public int getMethodIdsOff() {
		return methodIdsOff;
	}

	public int getMethodIdsSize() {
		return methodIdsSize;
	}

	public int getCallSiteOff() {
		return callSiteOff;
	}

	public int getMethodHandleOff() {
		return methodHandleOff;
	}
}
