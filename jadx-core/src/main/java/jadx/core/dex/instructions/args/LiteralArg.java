package jadx.core.dex.instructions.args;

import org.jetbrains.annotations.Nullable;

import jadx.core.codegen.TypeGen;
import jadx.core.utils.StringUtils;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class LiteralArg extends InsnArg {

	public static LiteralArg make(long value, ArgType type) {
		return new LiteralArg(value, type);
	}

	public static LiteralArg makeWithFixedType(long value, ArgType type) {
		return new LiteralArg(value, fixLiteralType(value, type));
	}

	private static ArgType fixLiteralType(long value, ArgType type) {
		if (value == 0 || type.isTypeKnown() || type.contains(PrimitiveType.LONG) || type.contains(PrimitiveType.DOUBLE)) {
			return type;
		}
		if (value == 1) {
			return ArgType.NARROW_NUMBERS;
		}
		return ArgType.NARROW_NUMBERS_NO_BOOL;
	}

	public static LiteralArg litFalse() {
		return new LiteralArg(0, ArgType.BOOLEAN);
	}

	public static LiteralArg litTrue() {
		return new LiteralArg(1, ArgType.BOOLEAN);
	}

	private final long literal;

	private LiteralArg(long value, ArgType type) {
		if (value != 0 && type.isObject()) {
			throw new JadxRuntimeException("Wrong literal type: " + type + " for value: " + value);
		}
		this.literal = value;
		this.type = type;
	}

	public long getLiteral() {
		return literal;
	}

	@Override
	public void setType(ArgType type) {
		super.setType(type);
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	public boolean isInteger() {
		switch (type.getPrimitiveType()) {
			case INT:
			case BYTE:
			case CHAR:
			case SHORT:
			case LONG:
				return true;
			default:
				return false;
		}
	}

	public boolean isNegative() {
		if (isInteger()) {
			return literal < 0;
		}
		if (type == ArgType.FLOAT) {
			float val = Float.intBitsToFloat(((int) literal));
			return val < 0 && Float.isFinite(val);
		}
		if (type == ArgType.DOUBLE) {
			double val = Double.longBitsToDouble(literal);
			return val < 0 && Double.isFinite(val);
		}
		return false;
	}

	@Nullable
	public LiteralArg negate() {
		long neg;
		if (isInteger()) {
			neg = -literal;
		} else if (type == ArgType.FLOAT) {
			float val = Float.intBitsToFloat(((int) literal));
			neg = Float.floatToIntBits(-val);
		} else if (type == ArgType.DOUBLE) {
			double val = Double.longBitsToDouble(literal);
			neg = Double.doubleToLongBits(-val);
		} else {
			return null;
		}
		return new LiteralArg(neg, type);
	}

	@Override
	public InsnArg duplicate() {
		return copyCommonParams(new LiteralArg(literal, type));
	}

	@Override
	public int hashCode() {
		return (int) (literal ^ literal >>> 32) + 31 * getType().hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		LiteralArg that = (LiteralArg) o;
		return literal == that.literal && getType().equals(that.getType());
	}

	@Override
	public String toString() {
		try {
			String value = TypeGen.literalToString(literal, getType(), StringUtils.getInstance(), true, false);
			if (getType().equals(ArgType.BOOLEAN) && (value.equals("true") || value.equals("false"))) {
				return value;
			}
			return '(' + value + ' ' + type + ')';
		} catch (JadxRuntimeException ex) {
			// can't convert literal to string
			return "(" + literal + ' ' + type + ')';
		}
	}
}
