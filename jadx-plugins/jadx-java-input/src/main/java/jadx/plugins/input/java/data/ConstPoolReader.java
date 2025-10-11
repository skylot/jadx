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
		JavaBootstrapMethodsAttr bootstrapMethodsAttr = clsData.loadClassAttribute(data, JavaAttrType.BOOTSTRAP_METHODS);
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
			case CLASS:
				return new EncodedValue(EncodedType.ENCODED_TYPE, getClass(idx));
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
		// parse modified UTF-8 according jvms-4.4.7
		StringBuffer sb = new StringBuffer(bytes.length);

		for(int i=0; i<bytes.length; i++) {
			int x = bytes[i] & 0xff;;

			if ((x & 128) == 0) { //4.4 ascii characters 1-127 (0 is encoded as 0xc0 0x80)
				sb.appendCodePoint(x); // 1 byte 7-Bit ascii (Table 4.4./4.5)
			}
			else {
				if( i+1 >= bytes.length)
					throw new RuntimeException("inconsistent byte array structure");

				int y = bytes[i+1] & 0xff;

				if (x == 0xc0 && y==0x80) //0 is encoded as 0xc0 0x80 (jvms-4.4.7)
				{
					sb.appendCodePoint(0);
					i++;
				}
				else if( (x & 0xE0) == 0xC0 && (y & 0xC0) == 0x80 ) {
					sb.appendCodePoint((int)((x & 0x1f) << 6) + (y & 0x3f)); //2 byte char (Table 4.8./4.9 )
					i++;
				}
				else if( i+2 < bytes.length)  {
					int z = bytes[i+2] & 0xff;

					if( (x & 0xF0) == 0xE0 && (y & 0xC0) == 0x80 && (z & 0xC0) == 0x80) {
						sb.appendCodePoint(((x & 0xf) << 12) + ((y & 0x3f) << 6) + (z & 0x3f)); // 3 byte char (Table 4.11/4.12)
						i += 2;
					} else if( i + 6 < bytes.length &&
							x == 0xED &&  					//u
							(y & 0xF0) == 0xA0  && 			//v
							(bytes[i+3] & 0xff) == 0xED &&  //x
							(bytes[i+4] & 0xF0) == 0xA0     //y
					) {
						//6 byte encoded Table 4.12.
						int u = x; //0
						int v = y; //1
						int w = z; //2
						x = bytes[i+3] & 0xff;
						y = bytes[i+4] & 0xff;
						z = bytes[i+5] & 0xff;

						if( x == 0xED && (y & 0xF0) == 0xA0) {
							sb.appendCodePoint(0x10000 + ((v & 0x0f) << 16) + ((w & 0x3f) << 10) + ((y & 0x0f) << 6) + (z & 0x3f));
							i += 5;
						}
						else
							throw new RuntimeException("inconsistent byte array structure");
					}
					else
						throw new RuntimeException("inconsistent byte array structure");
				}
			}
		}

		return sb.toString();
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
