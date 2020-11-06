package jadx.core.utils;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.InsnArg;
import jadx.core.dex.instructions.args.LiteralArg;
import jadx.core.dex.nodes.RootNode;

public class EncodedValueUtils {

	/**
	 * Return constant literal from {@code jadx.api.plugins.input.data.annotations.EncodedValue}
	 *
	 * @return LiteralArg, String, ArgType or null
	 */
	@Nullable
	public static Object convertToConstValue(RootNode root, EncodedValue encodedValue) {
		if (encodedValue == null) {
			return null;
		}
		Object value = encodedValue.getValue();
		switch (encodedValue.getType()) {
			case ENCODED_NULL:
				return InsnArg.lit(0, ArgType.OBJECT);
			case ENCODED_BOOLEAN:
				return Boolean.TRUE.equals(value) ? LiteralArg.litTrue() : LiteralArg.litFalse();
			case ENCODED_BYTE:
				return InsnArg.lit((Byte) value, ArgType.BYTE);
			case ENCODED_SHORT:
				return InsnArg.lit((Short) value, ArgType.SHORT);
			case ENCODED_CHAR:
				return InsnArg.lit((Character) value, ArgType.CHAR);
			case ENCODED_INT:
				return InsnArg.lit((Integer) value, ArgType.INT);
			case ENCODED_LONG:
				return InsnArg.lit((Long) value, ArgType.LONG);
			case ENCODED_FLOAT:
				return InsnArg.lit(Float.floatToIntBits((Float) value), ArgType.FLOAT);
			case ENCODED_DOUBLE:
				return InsnArg.lit(Double.doubleToLongBits((Double) value), ArgType.DOUBLE);
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
