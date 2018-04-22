package jadx.core.codegen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.api.JadxArgs;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.dex.nodes.IDexNode;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeGen {
	private static final Logger LOG = LoggerFactory.getLogger(TypeGen.class);

	private TypeGen() {
	}

	public static String signature(ArgType type) {
		PrimitiveType stype = type.getPrimitiveType();
		if (stype == PrimitiveType.OBJECT) {
			return Utils.makeQualifiedObjectName(type.getObject());
		}
		if (stype == PrimitiveType.ARRAY) {
			return '[' + signature(type.getArrayElement());
		}
		return stype.getShortName();
	}

	/**
	 * Convert literal value to string according to value type
	 *
	 * @throws JadxRuntimeException for incorrect type or literal value
	 */
	public static String literalToString(long lit, ArgType type, IDexNode dexNode) {
		return literalToString(lit, type, dexNode.root().getStringUtils());
	}

	@Deprecated
	public static String literalToString(long lit, ArgType type) {
		return literalToString(lit, type, new StringUtils(new JadxArgs()));
	}

	private static String literalToString(long lit, ArgType type, StringUtils stringUtils) {
		if (type == null || !type.isTypeKnown()) {
			String n = Long.toString(lit);
			if (Math.abs(lit) > 100) {
				n += "; // 0x" + Long.toHexString(lit)
						+ " float:" + Float.intBitsToFloat((int) lit)
						+ " double:" + Double.longBitsToDouble(lit);
			}
			return n;
		}

		switch (type.getPrimitiveType()) {
			case BOOLEAN:
				return lit == 0 ? "false" : "true";
			case CHAR:
				return stringUtils.unescapeChar((char) lit);
			case BYTE:
				return formatByte((byte) lit);
			case SHORT:
				return formatShort((short) lit);
			case INT:
				return formatInteger((int) lit);
			case LONG:
				return formatLong(lit);
			case FLOAT:
				return formatFloat(Float.intBitsToFloat((int) lit));
			case DOUBLE:
				return formatDouble(Double.longBitsToDouble(lit));

			case OBJECT:
			case ARRAY:
				if (lit != 0) {
					LOG.warn("Wrong object literal: {} for type: {}", lit, type);
					return Long.toString(lit);
				}
				return "null";

			default:
				throw new JadxRuntimeException("Unknown type in literalToString: " + type);
		}
	}

	public static String formatShort(short s) {
		if (s == Short.MAX_VALUE) {
			return "Short.MAX_VALUE";
		}
		if (s == Short.MIN_VALUE) {
			return "Short.MIN_VALUE";
		}
		return "(short) " + Short.toString(s);
	}

	public static String formatByte(byte b) {
		if (b == Byte.MAX_VALUE) {
			return "Byte.MAX_VALUE";
		}
		if (b == Byte.MIN_VALUE) {
			return "Byte.MIN_VALUE";
		}
		return "(byte) " + Byte.toString(b);
	}

	public static String formatInteger(int i) {
		if (i == Integer.MAX_VALUE) {
			return "Integer.MAX_VALUE";
		}
		if (i == Integer.MIN_VALUE) {
			return "Integer.MIN_VALUE";
		}
		return Integer.toString(i);
	}

	public static String formatLong(long l) {
		if (l == Long.MAX_VALUE) {
			return "Long.MAX_VALUE";
		}
		if (l == Long.MIN_VALUE) {
			return "Long.MIN_VALUE";
		}
		String str = Long.toString(l);
		if (Math.abs(l) >= Integer.MAX_VALUE) {
			str += "L";
		}
		return str;
	}

	public static String formatDouble(double d) {
		if (Double.isNaN(d)) {
			return "Double.NaN";
		}
		if (d == Double.NEGATIVE_INFINITY) {
			return "Double.NEGATIVE_INFINITY";
		}
		if (d == Double.POSITIVE_INFINITY) {
			return "Double.POSITIVE_INFINITY";
		}
		if (d == Double.MIN_VALUE) {
			return "Double.MIN_VALUE";
		}
		if (d == Double.MAX_VALUE) {
			return "Double.MAX_VALUE";
		}
		if (d == Double.MIN_NORMAL) {
			return "Double.MIN_NORMAL";
		}
		return Double.toString(d) + "d";
	}

	public static String formatFloat(float f) {
		if (Float.isNaN(f)) {
			return "Float.NaN";
		}
		if (f == Float.NEGATIVE_INFINITY) {
			return "Float.NEGATIVE_INFINITY";
		}
		if (f == Float.POSITIVE_INFINITY) {
			return "Float.POSITIVE_INFINITY";
		}
		if (f == Float.MIN_VALUE) {
			return "Float.MIN_VALUE";
		}
		if (f == Float.MAX_VALUE) {
			return "Float.MAX_VALUE";
		}
		if (f == Float.MIN_NORMAL) {
			return "Float.MIN_NORMAL";
		}
		return Float.toString(f) + "f";
	}

}
