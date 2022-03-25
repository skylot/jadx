package jadx.core.utils;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;

public class EncodedValueUtils {

	/**
	 * Return constant literal from {@code jadx.api.plugins.input.data.annotations.EncodedValue}
	 *
	 * @return LiteralArg, String, ArgType or null
	 */
	@Nullable
	public static Object convertToConstValue(EncodedValue encodedValue) {
		if (encodedValue == null) {
			return null;
		}
		Object value = encodedValue.getValue();
		switch (encodedValue.getType()) {
			case ENCODED_NULL:
				return InsnArg.literal(0, ArgType.OBJECT);
			case ENCODED_BOOLEAN:
				return Boolean.TRUE.equals(value) ? LiteralArg.litTrue() : LiteralArg.litFalse();
			case ENCODED_BYTE:
				return InsnArg.literal((Byte) value, ArgType.BYTE);
			case ENCODED_SHORT:
				return InsnArg.literal((Short) value, ArgType.SHORT);
			case ENCODED_CHAR:
				return InsnArg.literal((Character) value, ArgType.CHAR);
			case ENCODED_INT:
				return InsnArg.literal((Integer) value, ArgType.INT);
			case ENCODED_LONG:
				return InsnArg.literal((Long) value, ArgType.LONG);
			case ENCODED_FLOAT:
				return InsnArg.literal(Float.floatToIntBits((Float) value), ArgType.FLOAT);
			case ENCODED_DOUBLE:
				return InsnArg.literal(Double.doubleToLongBits((Double) value), ArgType.DOUBLE);
			case ENCODED_STRING:
				// noinspection RedundantCast
				return (String) value;

			case ENCODED_TYPE:
				return ArgType.parse((String) value);

			default:
				return null;
		}
	}
}
