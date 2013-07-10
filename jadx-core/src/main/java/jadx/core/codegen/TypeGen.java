package jadx.core.codegen;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.PrimitiveType;
import jadx.core.utils.StringUtils;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public class TypeGen {

	public static String translate(ClassGen clsGen, ArgType type) {
		final PrimitiveType stype = type.getPrimitiveType();
		if (stype == null)
			return type.toString();

		if (stype == PrimitiveType.OBJECT) {
			return clsGen.useClass(type);
		}
		if (stype == PrimitiveType.ARRAY) {
			return translate(clsGen, type.getArrayElement()) + "[]";
		}
		return stype.getLongName();
	}

	public static String signature(ArgType type) {
		final PrimitiveType stype = type.getPrimitiveType();
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
	 * @throws JadxRuntimeException
	 *             for incorrect type or literal value
	 */
	public static String literalToString(long lit, ArgType type) {
		if (type == null || !type.isTypeKnown()) {
			String n = Long.toString(lit);
			if (Math.abs(lit) > 100)
				n += "; // 0x" + Long.toHexString(lit)
						+ " float:" + Float.intBitsToFloat((int) lit)
						+ " double:" + Double.longBitsToDouble(lit);
			return n;
		}

		switch (type.getPrimitiveType()) {
			case BOOLEAN:
				return lit == 0 ? "false" : "true";
			case CHAR:
				return StringUtils.unescapeChar((char) lit);
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
				if (lit != 0)
					throw new JadxRuntimeException("Wrong object literal: " + type + " = " + lit);
				return "null";

			default:
				throw new JadxRuntimeException("Unknown type in literalToString: " + type);
		}
	}

	public static String formatShort(short s) {
		return "(short) " + wrapNegNum(s < 0, Short.toString(s));
	}

	public static String formatByte(byte b) {
		return "(byte) " + wrapNegNum(b < 0, Byte.toString(b));
	}

	public static String formatInteger(int i) {
		return wrapNegNum(i < 0, Integer.toString(i));
	}

	public static String formatDouble(double d) {
		return wrapNegNum(d < 0, Double.toString(d) + "d");
	}

	public static String formatFloat(float f) {
		return wrapNegNum(f < 0, Float.toString(f) + "f");
	}

	public static String formatLong(long lit) {
		String l = Long.toString(lit);
		if (lit == Long.MIN_VALUE || Math.abs(lit) >= Integer.MAX_VALUE)
			l += "L";
		return wrapNegNum(lit < 0, l);
	}

	private static String wrapNegNum(boolean lz, String str) {
		if (lz)
			return "(" + str + ")";
		else
			return str;
	}
}
