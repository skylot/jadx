package jadx.plugins.input.dex.sections.annotations;

import java.util.ArrayList;
import java.util.List;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.plugins.input.dex.DexException;
import jadx.plugins.input.dex.sections.SectionReader;

public class EncodedValueParser {

	private static final int ENCODED_BYTE = 0x00;
	private static final int ENCODED_SHORT = 0x02;
	private static final int ENCODED_CHAR = 0x03;
	private static final int ENCODED_INT = 0x04;
	private static final int ENCODED_LONG = 0x06;
	private static final int ENCODED_FLOAT = 0x10;
	private static final int ENCODED_DOUBLE = 0x11;
	private static final int ENCODED_METHOD_TYPE = 0x15;
	private static final int ENCODED_METHOD_HANDLE = 0x16;
	private static final int ENCODED_STRING = 0x17;
	private static final int ENCODED_TYPE = 0x18;
	private static final int ENCODED_FIELD = 0x19;
	private static final int ENCODED_ENUM = 0x1b;
	private static final int ENCODED_METHOD = 0x1a;
	private static final int ENCODED_ARRAY = 0x1c;
	private static final int ENCODED_ANNOTATION = 0x1d;
	private static final int ENCODED_NULL = 0x1e;
	private static final int ENCODED_BOOLEAN = 0x1f;

	static EncodedValue parseValue(SectionReader in, SectionReader ext) {
		int argAndType = in.readUByte();
		int type = argAndType & 0x1F;
		int arg = (argAndType & 0xE0) >> 5;
		int size = arg + 1;

		switch (type) {
			case ENCODED_NULL:
				return EncodedValue.NULL;

			case ENCODED_BOOLEAN:
				return new EncodedValue(EncodedType.ENCODED_BOOLEAN, arg == 1);
			case ENCODED_BYTE:
				return new EncodedValue(EncodedType.ENCODED_BYTE, in.readByte());

			case ENCODED_SHORT:
				return new EncodedValue(EncodedType.ENCODED_SHORT, (short) parseNumber(in, size, true));
			case ENCODED_CHAR:
				return new EncodedValue(EncodedType.ENCODED_CHAR, (char) parseUnsignedInt(in, size));
			case ENCODED_INT:
				return new EncodedValue(EncodedType.ENCODED_INT, (int) parseNumber(in, size, true));
			case ENCODED_LONG:
				return new EncodedValue(EncodedType.ENCODED_LONG, parseNumber(in, size, true));

			case ENCODED_FLOAT:
				return new EncodedValue(EncodedType.ENCODED_FLOAT, Float.intBitsToFloat((int) parseNumber(in, size, false, 4)));
			case ENCODED_DOUBLE:
				return new EncodedValue(EncodedType.ENCODED_DOUBLE, Double.longBitsToDouble(parseNumber(in, size, false, 8)));

			case ENCODED_STRING:
				return new EncodedValue(EncodedType.ENCODED_STRING, ext.getString(parseUnsignedInt(in, size)));

			case ENCODED_TYPE:
				return new EncodedValue(EncodedType.ENCODED_TYPE, ext.getType(parseUnsignedInt(in, size)));

			case ENCODED_FIELD:
			case ENCODED_ENUM:
				return new EncodedValue(EncodedType.ENCODED_FIELD, ext.getFieldRef(parseUnsignedInt(in, size)));

			case ENCODED_ARRAY:
				return new EncodedValue(EncodedType.ENCODED_ARRAY, parseEncodedArray(in, ext));

			case ENCODED_ANNOTATION:
				return new EncodedValue(EncodedType.ENCODED_ANNOTATION, AnnotationsParser.readAnnotation(in, ext, false));

			case ENCODED_METHOD:
				return new EncodedValue(EncodedType.ENCODED_METHOD, ext.getMethodRef(parseUnsignedInt(in, size)));

			case ENCODED_METHOD_TYPE:
				return new EncodedValue(EncodedType.ENCODED_METHOD_TYPE, ext.getMethodProto(parseUnsignedInt(in, size)));

			case ENCODED_METHOD_HANDLE:
				return new EncodedValue(EncodedType.ENCODED_METHOD_HANDLE, ext.getMethodHandle(parseUnsignedInt(in, size)));

			default:
				throw new DexException("Unknown encoded value type: 0x" + Integer.toHexString(type));
		}
	}

	public static List<EncodedValue> parseEncodedArray(SectionReader in, SectionReader ext) {
		int count = in.readUleb128();
		List<EncodedValue> values = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			values.add(parseValue(in, ext));
		}
		return values;
	}

	private static int parseUnsignedInt(SectionReader in, int byteCount) {
		return (int) parseNumber(in, byteCount, false, 0);
	}

	private static long parseNumber(SectionReader in, int byteCount, boolean isSignExtended) {
		return parseNumber(in, byteCount, isSignExtended, 0);
	}

	private static long parseNumber(SectionReader in, int byteCount, boolean isSignExtended, int fillOnRight) {
		long result = 0;
		long last = 0;
		for (int i = 0; i < byteCount; i++) {
			last = in.readUByte();
			result |= last << (i * 8);
		}
		if (fillOnRight != 0) {
			for (int i = byteCount; i < fillOnRight; i++) {
				result <<= 8;
			}
		} else {
			if (isSignExtended && (last & 0x80) != 0) {
				for (int i = byteCount; i < 8; i++) {
					result |= (long) 0xFF << (i * 8);
				}
			}
		}
		return result;
	}

}
