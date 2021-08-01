package jadx.plugins.input.dex.sections;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.api.plugins.input.data.impl.CallSite;
import jadx.api.plugins.input.data.impl.FieldRefHandle;
import jadx.api.plugins.input.data.impl.MethodRefHandle;
import jadx.plugins.input.dex.DexReader;
import jadx.plugins.input.dex.sections.annotations.EncodedValueParser;
import jadx.plugins.input.dex.utils.Leb128;
import jadx.plugins.input.dex.utils.MUtf8;

import static jadx.plugins.input.dex.sections.DexConsts.NO_INDEX;

public class SectionReader {
	private final ByteBuffer buf;
	private final DexReader dexReader;
	private int offset;

	public SectionReader(DexReader dexReader, int off) {
		this.dexReader = dexReader;
		this.offset = off;
		this.buf = duplicate(dexReader.getBuf(), off);
	}

	private SectionReader(SectionReader sectionReader, int off) {
		this(sectionReader.dexReader, off);
	}

	public SectionReader copy() {
		return new SectionReader(this, offset);
	}

	public SectionReader copy(int off) {
		return new SectionReader(this, off);
	}

	public byte[] getByteCode(int start, int len) {
		int pos = buf.position();
		buf.position(start);
		byte[] bytes = readByteArray(len);
		buf.position(pos);
		return bytes;
	}

	private static ByteBuffer duplicate(ByteBuffer baseBuffer, int off) {
		ByteBuffer dupBuf = baseBuffer.duplicate();
		dupBuf.order(ByteOrder.LITTLE_ENDIAN);
		dupBuf.position(off);
		return dupBuf;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}

	public void shiftOffset(int shift) {
		this.offset += shift;
	}

	public SectionReader pos(int pos) {
		buf.position(offset + pos);
		return this;
	}

	public SectionReader absPos(int pos) {
		buf.position(pos);
		return this;
	}

	public int getAbsPos() {
		return buf.position();
	}

	public void skip(int skip) {
		int pos = buf.position();
		buf.position(pos + skip);
	}

	public int readInt() {
		return buf.getInt();
	}

	public long readLong() {
		return buf.getLong();
	}

	public byte readByte() {
		return buf.get();
	}

	public int readUByte() {
		return buf.get() & 0xFF;
	}

	public int readUShort() {
		return buf.getShort() & 0xFFFF;
	}

	public int readShort() {
		return buf.getShort();
	}

	public byte[] readByteArray(int len) {
		byte[] arr = new byte[len];
		buf.get(arr);
		return arr;
	}

	public int[] readUShortArray(int size) {
		int[] arr = new int[size];
		for (int i = 0; i < size; i++) {
			arr[i] = readUShort();
		}
		return arr;
	}

	public String readString(int len) {
		return new String(readByteArray(len), StandardCharsets.US_ASCII);
	}

	private List<String> readTypeListAt(int paramsOff) {
		if (paramsOff == 0) {
			return Collections.emptyList();
		}
		return absPos(paramsOff).readTypeList();
	}

	public List<String> readTypeList() {
		int size = readInt();
		if (size == 0) {
			return Collections.emptyList();
		}
		int[] typeIds = readUShortArray(size);
		List<String> types = new ArrayList<>(size);
		for (int typeId : typeIds) {
			types.add(getType(typeId));
		}
		return types;
	}

	@Nullable
	public String getType(int idx) {
		if (idx == NO_INDEX) {
			return null;
		}
		int typeIdsOff = dexReader.getHeader().getTypeIdsOff();
		absPos(typeIdsOff + idx * 4);
		int strIdx = readInt();
		return getString(strIdx);
	}

	@Nullable
	public String getString(int idx) {
		if (idx == NO_INDEX) {
			return null;
		}
		// TODO: make string pool cache?
		int stringIdsOff = dexReader.getHeader().getStringIdsOff();
		absPos(stringIdsOff + idx * 4);
		int strOff = readInt();
		absPos(strOff);
		return MUtf8.decode(this);
	}

	public IFieldRef getFieldRef(int idx) {
		DexFieldData fieldData = new DexFieldData(null);
		int clsTypeIdx = fillFieldData(fieldData, idx);
		fieldData.setParentClassType(getType(clsTypeIdx));
		return fieldData;
	}

	public int fillFieldData(DexFieldData fieldData, int idx) {
		int fieldIdsOff = dexReader.getHeader().getFieldIdsOff();
		absPos(fieldIdsOff + idx * 8);
		int classTypeIdx = readUShort();
		int typeIdx = readUShort();
		int nameIdx = readInt();
		fieldData.setType(getType(typeIdx));
		fieldData.setName(getString(nameIdx));
		return classTypeIdx;
	}

	public DexMethodRef getMethodRef(int idx) {
		DexMethodRef methodRef = new DexMethodRef();
		initMethodRef(idx, methodRef);
		return methodRef;
	}

	public ICallSite getCallSite(int idx, SectionReader ext) {
		int callSiteOff = dexReader.getHeader().getCallSiteOff();
		absPos(callSiteOff + idx * 4);
		absPos(readInt());
		return new CallSite(EncodedValueParser.parseEncodedArray(this, ext));
	}

	public IMethodHandle getMethodHandle(int idx) {
		int methodHandleOff = dexReader.getHeader().getMethodHandleOff();
		absPos(methodHandleOff + idx * 8);
		MethodHandleType handleType = getMethodHandleType(readUShort());
		skip(2);
		int refId = readUShort();
		if (handleType.isField()) {
			return new FieldRefHandle(handleType, getFieldRef(refId));
		}
		return new MethodRefHandle(handleType, getMethodRef(refId));
	}

	private MethodHandleType getMethodHandleType(int type) {
		switch (type) {
			case 0x00:
				return MethodHandleType.STATIC_PUT;
			case 0x01:
				return MethodHandleType.STATIC_GET;
			case 0x02:
				return MethodHandleType.INSTANCE_PUT;
			case 0x03:
				return MethodHandleType.INSTANCE_GET;
			case 0x04:
				return MethodHandleType.INVOKE_STATIC;
			case 0x05:
				return MethodHandleType.INVOKE_INSTANCE;
			case 0x06:
				return MethodHandleType.INVOKE_CONSTRUCTOR;
			case 0x07:
				return MethodHandleType.INVOKE_DIRECT;
			case 0x08:
				return MethodHandleType.INVOKE_INTERFACE;
			default:
				throw new IllegalArgumentException("Unknown method handle type: 0x" + Integer.toHexString(type));
		}
	}

	public void initMethodRef(int idx, DexMethodRef methodRef) {
		methodRef.initUniqId(dexReader, idx);
		methodRef.setDexIdx(idx);
		methodRef.setSectionReader(this);
	}

	public void loadMethodRef(DexMethodRef methodRef, int idx) {
		DexHeader header = dexReader.getHeader();
		int methodIdsOff = header.getMethodIdsOff();
		absPos(methodIdsOff + idx * 8);
		int classTypeIdx = readUShort();
		int protoIdx = readUShort();
		int nameIdx = readInt();

		int protoIdsOff = header.getProtoIdsOff();
		absPos(protoIdsOff + protoIdx * 12);
		skip(4); // shortyIdx
		int returnTypeIdx = readInt();
		int paramsOff = readInt();

		List<String> argTypes = readTypeListAt(paramsOff);
		methodRef.setParentClassType(getType(classTypeIdx));
		methodRef.setName(getString(nameIdx));
		methodRef.setReturnType(getType(returnTypeIdx));
		methodRef.setArgTypes(argTypes);
	}

	public DexMethodProto getMethodProto(int idx) {
		int protoIdsOff = dexReader.getHeader().getProtoIdsOff();
		absPos(protoIdsOff + idx * 12);
		skip(4); // shortyIdx
		int returnTypeIdx = readInt();
		int paramsOff = readInt();
		return new DexMethodProto(readTypeListAt(paramsOff), getType(returnTypeIdx));
	}

	public List<String> getMethodParamTypes(int idx) {
		DexHeader header = dexReader.getHeader();
		int methodIdsOff = header.getMethodIdsOff();
		absPos(methodIdsOff + idx * 8 + 2);
		int protoIdx = readUShort();

		int protoIdsOff = header.getProtoIdsOff();
		absPos(protoIdsOff + protoIdx * 12 + 8);
		int paramsOff = readInt();

		if (paramsOff == 0) {
			return Collections.emptyList();
		}
		return absPos(paramsOff).readTypeList();
	}

	public DexReader getDexReader() {
		return dexReader;
	}

	public int readUleb128() {
		return Leb128.readUnsignedLeb128(this);
	}

	public int readUleb128p1() {
		return Leb128.readUnsignedLeb128(this) - 1;
	}

	public int readSleb128() {
		return Leb128.readSignedLeb128(this);
	}

	@Override
	public String toString() {
		return "SectionReader{buf=" + buf + ", offset=" + offset + '}';
	}
}
