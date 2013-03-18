package jadx.dex.nodes.parser;

import jadx.dex.info.FieldInfo;
import jadx.dex.info.MethodInfo;
import jadx.dex.nodes.DexNode;
import jadx.utils.exceptions.DecodeException;

import java.util.ArrayList;
import java.util.List;

import com.android.dx.io.DexBuffer.Section;
import com.android.dx.io.EncodedValueReader;
import com.android.dx.util.Leb128Utils;

public class EncValueParser extends EncodedValueReader {

	private final DexNode dex;

	public EncValueParser(DexNode dex, Section in) {
		super(in);
		this.dex = dex;
	}

	public Object parseValue() throws DecodeException {
		int argAndType = in.readByte() & 0xff;
		int type = argAndType & 0x1f;
		int arg = (argAndType & 0xe0) >> 5;
		int size = arg + 1;

		switch (type) {
			case ENCODED_NULL:
				return null;

			case ENCODED_BOOLEAN:
				return Boolean.valueOf(arg == 1);
			case ENCODED_BYTE:
				return Byte.valueOf((byte) parseNumber(size));
			case ENCODED_SHORT:
				return Short.valueOf((short) parseNumber(size));
			case ENCODED_CHAR:
				return Character.valueOf((char) parseNumber(size));
			case ENCODED_INT:
				return Integer.valueOf((int) parseNumber(size));
			case ENCODED_LONG:
				return Long.valueOf(parseNumber(size));
			case ENCODED_FLOAT:
				return Float.intBitsToFloat((int) parseNumber(size));
			case ENCODED_DOUBLE:
				return Double.longBitsToDouble(parseNumber(size));

			case ENCODED_STRING:
				return dex.getString((int) parseNumber(size));

			case ENCODED_TYPE:
				return dex.getType((int) parseNumber(size));

			case ENCODED_METHOD:
				return MethodInfo.fromDex(dex, (int) parseNumber(size));

			case ENCODED_FIELD:
			case ENCODED_ENUM:
				return FieldInfo.fromDex(dex, (int) parseNumber(size));

			case ENCODED_ARRAY:
				int count = Leb128Utils.readUnsignedLeb128(in);
				List<Object> values = new ArrayList<Object>(count);
				for (int i = 0; i < count; i++) {
					values.add(parseValue());
				}
				return values;

			case ENCODED_ANNOTATION:
				return AnnotationsParser.readAnnotation(dex, (Section) in, false);
		}
		throw new DecodeException("Unknown encoded value type: 0x" + Integer.toHexString(type));
	}

	private long parseNumber(int byteCount) {
		long result = 0;
		int shift = 0;
		for (int i = 0; i < byteCount; i++) {
			result |= (long) (in.readByte() & 0xff) << shift;
			shift += 8;
		}
		return result;
	}
}
