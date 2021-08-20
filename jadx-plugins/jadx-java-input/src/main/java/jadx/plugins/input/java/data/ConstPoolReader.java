package jadx.plugins.input.java.data;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.ICallSite;
import jadx.api.plugins.input.data.IFieldRef;
import jadx.api.plugins.input.data.IMethodHandle;
import jadx.api.plugins.input.data.IMethodRef;
import jadx.api.plugins.input.data.MethodHandleType;
import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.impl.CallSite;
import jadx.api.plugins.input.data.impl.FieldRefHandle;
import jadx.api.plugins.input.data.impl.MethodRefHandle;
import jadx.plugins.input.java.JavaClassReader;
import jadx.plugins.input.java.data.attributes.JavaAttrType;
import jadx.plugins.input.java.data.attributes.types.JavaBootstrapMethodsAttr;
import jadx.plugins.input.java.data.attributes.types.data.RawBootstrapMethod;
import jadx.plugins.input.java.utils.DescriptorParser;
import jadx.plugins.input.java.utils.JavaClassParseException;

public class ConstPoolReader {
	private final JavaClassReader clsReader;
	private final JavaClassData clsData;
	private final DataReader data;
	private final ClassOffsets offsets;

	public ConstPoolReader(JavaClassReader clsReader, JavaClassData javaClassData, DataReader data, ClassOffsets offsets) {
		this.clsReader = clsReader;
		this.clsData = javaClassData;
		this.data = data;
		this.offsets = offsets;
	}

	@Nullable
	public String getClass(int idx) {
		jumpToData(idx);
		int nameIdx = data.readU2();
		return fixType(getUtf8(nameIdx));
	}

	public IFieldRef getFieldRef(int idx) {
		jumpToData(idx);
		int clsIdx = data.readU2();
		int nameTypeIdx = data.readU2();
		jumpToData(nameTypeIdx);
		int nameIdx = data.readU2();
		int typeIdx = data.readU2();

		JavaFieldData fieldData = new JavaFieldData();
		fieldData.setParentClassType(getClass(clsIdx));
		fieldData.setName(getUtf8(nameIdx));
		fieldData.setType(getUtf8(typeIdx));
		return fieldData;
	}

	public String getFieldType(int idx) {
		jumpToData(idx);
		data.skip(2);
		int nameTypeIdx = data.readU2();
		jumpToData(nameTypeIdx);
		data.skip(2);
		int typeIdx = data.readU2();
		return getUtf8(typeIdx);
	}

	public IMethodRef getMethodRef(int idx) {
		jumpToData(idx);
		int clsIdx = data.readU2();
		int nameTypeIdx = data.readU2();
		jumpToData(nameTypeIdx);
		int nameIdx = data.readU2();
		int descIdx = data.readU2();

		JavaMethodRef mthRef = new JavaMethodRef();
		mthRef.initUniqId(clsReader, idx, true);
		mthRef.setParentClassType(getClass(clsIdx));
		mthRef.setName(getUtf8(nameIdx));
		mthRef.setDescr(getUtf8(descIdx));
		return mthRef;
	}

	public ICallSite getCallSite(int idx) {
		ConstantType constType = jumpToConst(idx);
		switch (constType) {
			case INVOKE_DYNAMIC:
				int bootstrapMthIdx = data.readU2();
				int nameAndTypeIdx = data.readU2();
				jumpToData(nameAndTypeIdx);
				int nameIdx = data.readU2();
				int descIdx = data.readU2();
				return resolveMethodCallSite(bootstrapMthIdx, nameIdx, descIdx);
			case DYNAMIC:
				throw new JavaClassParseException("Field call site not yet implemented");
			default:
				throw new JavaClassParseException("Unexpected tag type for call site: " + constType);
		}
	}

	private CallSite resolveMethodCallSite(int bootstrapMthIdx, int nameIdx, int descIdx) {
		JavaBootstrapMethodsAttr bootstrapMethodsAttr = clsData.loadAttribute(data, JavaAttrType.BOOTSTRAP_METHODS);
		if (bootstrapMethodsAttr == null) {
			throw new JavaClassParseException("Unexpected missing BootstrapMethods attribute");
		}
		RawBootstrapMethod rawBootstrapMethod = bootstrapMethodsAttr.getList().get(bootstrapMthIdx);

		List<EncodedValue> values = new ArrayList<>(6);
		values.add(new EncodedValue(EncodedType.ENCODED_METHOD_HANDLE, getMethodHandle(rawBootstrapMethod.getMethodHandleIdx())));
		values.add(new EncodedValue(EncodedType.ENCODED_STRING, getUtf8(nameIdx)));
		values.add(new EncodedValue(EncodedType.ENCODED_METHOD_TYPE, DescriptorParser.parseToMethodProto(getUtf8(descIdx))));
		for (int argConstIdx : rawBootstrapMethod.getArgs()) {
			values.add(readAsEncodedValue(argConstIdx));
		}
		return new CallSite(values);
	}

	private IMethodHandle getMethodHandle(int idx) {
		jumpToData(idx);
		int kind = data.readU1();
		int refIdx = data.readU2();
		MethodHandleType handleType = convertMethodHandleKind(kind);
		if (handleType.isField()) {
			return new FieldRefHandle(handleType, getFieldRef(refIdx));
		}
		return new MethodRefHandle(handleType, getMethodRef(refIdx));
	}

	private MethodHandleType convertMethodHandleKind(int kind) {
		switch (kind) {
			case 1:
				return MethodHandleType.STATIC_PUT;
			case 2:
				return MethodHandleType.STATIC_GET;
			case 3:
				return MethodHandleType.INSTANCE_PUT;
			case 4:
				return MethodHandleType.INSTANCE_GET;
			case 5:
				return MethodHandleType.INVOKE_INSTANCE;
			case 6:
				return MethodHandleType.INVOKE_STATIC;
			case 7:
				return MethodHandleType.INVOKE_DIRECT;
			case 8:
				return MethodHandleType.INVOKE_CONSTRUCTOR;
			case 9:
				return MethodHandleType.INVOKE_INTERFACE;
			default:
				throw new IllegalArgumentException("Unknown method handle type: " + kind);
		}
	}

	public String getUtf8(int idx) {
		if (idx == 0) {
			return null;
		}
		jumpToData(idx);
		return readString();
	}

	public ConstantType jumpToConst(int idx) {
		jumpToTag(idx);
		return ConstantType.getTypeByTag(data.readU1());
	}

	public String readString() {
		int len = data.readU2();
		byte[] bytes = data.readBytes(len);
		return parseString(bytes);
	}

	public int readU2() {
		return data.readU2();
	}

	public int readU4() {
		return data.readU4();
	}

	public long readU8() {
		return data.readU8();
	}

	public int getInt(int idx) {
		jumpToData(idx);
		return data.readS4();
	}

	public long getLong(int idx) {
		jumpToData(idx);
		return data.readS8();
	}

	public double getDouble(int idx) {
		jumpToData(idx);
		return Double.longBitsToDouble(data.readU8());
	}

	public float getFloat(int idx) {
		jumpToData(idx);
		return Float.intBitsToFloat(data.readU4());
	}

	public EncodedValue readAsEncodedValue(int idx) {
		ConstantType constantType = jumpToConst(idx);
		switch (constantType) {
			case UTF8:
				return new EncodedValue(EncodedType.ENCODED_STRING, readString());
			case STRING:
				return new EncodedValue(EncodedType.ENCODED_STRING, getUtf8(readU2()));
			case INTEGER:
				return new EncodedValue(EncodedType.ENCODED_INT, data.readS4());
			case FLOAT:
				return new EncodedValue(EncodedType.ENCODED_FLOAT, Float.intBitsToFloat(data.readU4()));
			case LONG:
				return new EncodedValue(EncodedType.ENCODED_LONG, data.readS8());
			case DOUBLE:
				return new EncodedValue(EncodedType.ENCODED_DOUBLE, Double.longBitsToDouble(data.readU8()));
			case METHOD_TYPE:
				return new EncodedValue(EncodedType.ENCODED_METHOD_TYPE, DescriptorParser.parseToMethodProto(getUtf8(readU2())));
			case METHOD_HANDLE:
				return new EncodedValue(EncodedType.ENCODED_METHOD_HANDLE, getMethodHandle(idx));

			default:
				throw new JavaClassParseException("Can't encode constant " + constantType + " as encoded value");
		}
	}

	@NotNull
	private String parseString(byte[] bytes) {
		// TODO: parse modified UTF-8
		return new String(bytes, StandardCharsets.UTF_8);
	}

	private String fixType(String clsName) {
		switch (clsName.charAt(0)) {
			case '[':
				return clsName;

			case 'L':
			case 'T':
				if (clsName.endsWith(";")) {
					return clsName;
				}
				break;
		}
		return 'L' + clsName + ';';
	}

	private void jumpToData(int idx) {
		data.absPos(offsets.getOffsetOfConstEntry(idx));
	}

	private void jumpToTag(int idx) {
		data.absPos(offsets.getOffsetOfConstEntry(idx) - 1);
	}
}
