package jadx.core.dex.nodes.parser;

import java.util.ArrayList;
import java.util.List;

import com.android.dex.Dex.Section;
import com.android.dex.Leb128;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.exceptions.DecodeException;

public class EncValueParser {

	private static final int ENCODED_BYTE = 0x00;
	private static final int ENCODED_SHORT = 0x02;
	private static final int ENCODED_CHAR = 0x03;
	private static final int ENCODED_INT = 0x04;
	private static final int ENCODED_LONG = 0x06;
	private static final int ENCODED_FLOAT = 0x10;
	private static final int ENCODED_DOUBLE = 0x11;
	private static final int ENCODED_STRING = 0x17;
	private static final int ENCODED_TYPE = 0x18;
	private static final int ENCODED_FIELD = 0x19;
	private static final int ENCODED_ENUM = 0x1b;
	private static final int ENCODED_METHOD = 0x1a;
	private static final int ENCODED_ARRAY = 0x1c;
	private static final int ENCODED_ANNOTATION = 0x1d;
	private static final int ENCODED_NULL = 0x1e;
	private static final int ENCODED_BOOLEAN = 0x1f;

	protected final Section in;

	private final DexNode dex;

	public EncValueParser(DexNode dex, Section in) {
		this.in = in;
		this.dex = dex;
	}

	public Object parseValue() throws DecodeException {
		int argAndType = readByte();
		int type = argAndType & 0x1F;
		int arg = (argAndType & 0xE0) >> 5;
		int size = arg + 1;

		switch (type) {
			case ENCODED_NULL:
				return null;

			case ENCODED_BOOLEAN:
				return arg == 1;
			case ENCODED_BYTE:
				return in.readByte();

			case ENCODED_SHORT:
				return (short) parseNumber(size, true);
			case ENCODED_CHAR:
				return (char) parseUnsignedInt(size);
			case ENCODED_INT:
				return (int) parseNumber(size, true);
			case ENCODED_LONG:
				return parseNumber(size, true);

			case ENCODED_FLOAT:
				return Float.intBitsToFloat((int) parseNumber(size, false, 4));
			case ENCODED_DOUBLE:
				return Double.longBitsToDouble(parseNumber(size, false, 8));

			case ENCODED_STRING:
				return dex.getString(parseUnsignedInt(size));

			case ENCODED_TYPE:
				return dex.getType(parseUnsignedInt(size));

			case ENCODED_METHOD:
				return MethodInfo.fromDex(dex, parseUnsignedInt(size));

			case ENCODED_FIELD:
			case ENCODED_ENUM:
				return FieldInfo.fromDex(dex, parseUnsignedInt(size));

			case ENCODED_ARRAY:
				int count = Leb128.readUnsignedLeb128(in);
				List<Object> values = new ArrayList<>(count);
				for (int i = 0; i < count; i++) {
					values.add(parseValue());
				}
				return values;

			case ENCODED_ANNOTATION:
				return AnnotationsParser.readAnnotation(dex, in, false);

			default:
				throw new DecodeException("Unknown encoded value type: 0x" + Integer.toHexString(type));
		}
	}

	private int parseUnsignedInt(int byteCount) {
		return (int) parseNumber(byteCount, false, 0);
	}

	private long parseNumber(int byteCount, boolean isSignExtended) {
		return parseNumber(byteCount, isSignExtended, 0);
	}

	private long parseNumber(int byteCount, boolean isSignExtended, int fillOnRight) {
		long result = 0;
		long last = 0;
		for (int i = 0; i < byteCount; i++) {
			last = readByte();
			result |= last << i * 8;
		}
		if (fillOnRight != 0) {
			for (int i = byteCount; i < fillOnRight; i++) {
				result <<= 8;
			}
		} else {
			if (isSignExtended && (last & 0x80) != 0) {
				for (int i = byteCount; i < 8; i++) {
					result |= (long) 0xFF << i * 8;
				}
			}
		}
		return result;
	}

	private int readByte() {
		return in.readByte() & 0xFF;
	}
}
