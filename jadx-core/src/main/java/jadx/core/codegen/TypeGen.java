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
				return stringUtils.formatByte(lit, cast);
			case SHORT:
				return stringUtils.formatShort(lit, cast);
			case INT:
				return stringUtils.formatInteger(lit, cast);
			case LONG:
				return stringUtils.formatLong(lit, cast);
			case FLOAT:
				return StringUtils.formatFloat(Float.intBitsToFloat((int) lit));
			case DOUBLE:
				return StringUtils.formatDouble(Double.longBitsToDouble(lit));

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
}
