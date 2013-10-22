package jadx.core.dex.nodes.parser;

import jadx.core.dex.info.FieldInfo;
import jadx.core.dex.info.MethodInfo;
import jadx.core.dex.nodes.DexNode;
import jadx.core.utils.exceptions.DecodeException;

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
		int argAndType = in.readByte() & 0xFF;
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
				return (char) parseNumber(size, false);
			case ENCODED_INT:
				return (int) parseNumber(size, true);
			case ENCODED_LONG:
				return parseNumber(size, true);
			case ENCODED_FLOAT:
				return Float.intBitsToFloat((int) parseNumber(size, false));
			case ENCODED_DOUBLE:
				return Double.longBitsToDouble(parseNumber(size, false));

			case ENCODED_STRING:
				return dex.getString((int) parseNumber(size, false));

			case ENCODED_TYPE:
				return dex.getType((int) parseNumber(size, false));

			case ENCODED_METHOD:
				return MethodInfo.fromDex(dex, (int) parseNumber(size, false));

			case ENCODED_FIELD:
			case ENCODED_ENUM:
				return FieldInfo.fromDex(dex, (int) parseNumber(size, false));

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

	private long parseNumber(int byteCount, boolean isSignExtended) {
		long result = 0;
		int shift = 8;
		int first = in.readByte() & 0xFF;
		if (isSignExtended && (first & 0x80) != 0) {
			result = ~result << shift;
		}
		result |= (long) first;
		for (int i = 1; i < byteCount; i++) {
			result |= (long) (in.readByte() & 0xFF) << shift;
			shift += 8;
		}
		return result;
	}
}
