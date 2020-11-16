package jadx.core.codegen;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jadx.core.dex.attributes.AFlag;
import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.instructions.args.LiteralArg;
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
	 * Convert literal arg to string (preferred method)
	 */
	public static String literalToString(LiteralArg arg, IDexNode dexNode, boolean fallback) {
		return literalToString(arg.getLiteral(), arg.getType(),
				dexNode.root().getStringUtils(),
				fallback,
				arg.contains(AFlag.EXPLICIT_PRIMITIVE_TYPE));
	}

	/**
	 * Convert literal value to string according to value type
	 *
	 * @throws JadxRuntimeException for incorrect type or literal value
	 */
	public static String literalToString(long lit, ArgType type, IDexNode dexNode, boolean fallback) {
		return literalToString(lit, type, dexNode.root().getStringUtils(), fallback, false);
	}

	public static String literalToString(long lit, ArgType type, StringUtils stringUtils, boolean fallback, boolean cast) {
		if (type == null || !type.isTypeKnown()) {
			String n = Long.toString(lit);
			if (fallback && Math.abs(lit) > 100) {
				StringBuilder sb = new StringBuilder();
				sb.append(n).append("(0x").append(Long.toHexString(lit));
				if (type == null || type.contains(PrimitiveType.FLOAT)) {
					sb.append(", float:").append(Float.intBitsToFloat((int) lit));
				}
				if (type == null || type.contains(PrimitiveType.DOUBLE)) {
					sb.append(", double:").append(Double.longBitsToDouble(lit));
				}
				sb.append(')');
				return sb.toString();
			}
			return n;
		}

		switch (type.getPrimitiveType()) {
			case BOOLEAN:
				return lit == 0 ? "false" : "true";
			case CHAR:
				return stringUtils.unescapeChar((char) lit, cast);
			case BYTE:
				return formatByte(lit, cast);
			case SHORT:
				return formatShort(lit, cast);
			case INT:
				return formatInteger(lit, cast);
			case LONG:
				return formatLong(lit, cast);
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

	@Nullable
	public static String literalToRawString(LiteralArg arg) {
		ArgType type = arg.getType();
		if (type == null) {
			return null;
		}
		long lit = arg.getLiteral();
		switch (type.getPrimitiveType()) {
			case BOOLEAN:
				return lit == 0 ? "false" : "true";
			case CHAR:
				return String.valueOf((char) lit);

			case BYTE:
			case SHORT:
			case INT:
			case LONG:
				return Long.toString(lit);

			case FLOAT:
				return Float.toString(Float.intBitsToFloat((int) lit));
			case DOUBLE:
				return Double.toString(Double.longBitsToDouble(lit));

			case OBJECT:
			case ARRAY:
				if (lit != 0) {
					LOG.warn("Wrong object literal: {} for type: {}", lit, type);
					return Long.toString(lit);
				}
				return "null";

			default:
				return null;
		}
	}

	public static String formatShort(long l, boolean cast) {
		if (l == Short.MAX_VALUE) {
			return "Short.MAX_VALUE";
		}
		if (l == Short.MIN_VALUE) {
			return "Short.MIN_VALUE";
		}
		String str = Long.toString(l);
		return cast ? "(short) " + str : str;
	}

	public static String formatByte(long l, boolean cast) {
		if (l == Byte.MAX_VALUE) {
			return "Byte.MAX_VALUE";
		}
		if (l == Byte.MIN_VALUE) {
			return "Byte.MIN_VALUE";
		}
		String str = Long.toString(l);
		return cast ? "(byte) " + str : str;
	}

	public static String formatInteger(long l, boolean cast) {
		if (l == Integer.MAX_VALUE) {
			return "Integer.MAX_VALUE";
		}
		if (l == Integer.MIN_VALUE) {
			return "Integer.MIN_VALUE";
		}
		String str = Long.toString(l);
		return cast ? "(int) " + str : str;
	}

	public static String formatLong(long l, boolean cast) {
		if (l == Long.MAX_VALUE) {
			return "Long.MAX_VALUE";
		}
		if (l == Long.MIN_VALUE) {
			return "Long.MIN_VALUE";
		}
		String str = Long.toString(l);
		if (cast || Math.abs(l) >= Integer.MAX_VALUE) {
			return str + 'L';
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
		return Double.toString(d) + 'd';
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
		return Float.toString(f) + 'f';
	}
}
