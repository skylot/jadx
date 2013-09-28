package jadx.core.dex.instructions.args;

import jadx.core.codegen.TypeGen;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class LiteralArg extends InsnArg {

	private final long literal;

	public LiteralArg(long value, ArgType type) {
		if (value != 0) {
			if (type.isObject()) {
				throw new JadxRuntimeException("Wrong literal type: " + type + " for value: " + value);
			} else if (!type.isTypeKnown()
					&& !type.contains(PrimitiveType.LONG)
					&& !type.contains(PrimitiveType.DOUBLE)) {
				ArgType m = ArgType.merge(type, ArgType.NARROW_NUMBERS);
				if (m != null) {
					type = m;
				}
			}
		}
		this.literal = value;
		this.typedVar = new TypedVar(type);
	}

	public long getLiteral() {
		return literal;
	}

	@Override
	public boolean isLiteral() {
		return true;
	}

	public boolean isInteger() {
		PrimitiveType type = typedVar.getType().getPrimitiveType();
		return (type == PrimitiveType.INT
				|| type == PrimitiveType.BYTE
				|| type == PrimitiveType.CHAR
				|| type == PrimitiveType.SHORT
				|| type == PrimitiveType.LONG);
	}

	@Override
	public String toString() {
		try {
			return "(" + TypeGen.literalToString(literal, getType()) + " " + typedVar + ")";
		} catch (JadxRuntimeException ex) {
			// can't convert literal to string
			return "(" + literal + " " + typedVar + ")";
		}
	}
}
