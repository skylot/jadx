package jadx.core.dex.instructions.args;

import jadx.core.codegen.TypeGen;
import jadx.core.utils.exceptions.JadxRuntimeException;

public final class LiteralArg extends InsnArg {

	private final long literal;

	public LiteralArg(long value, ArgType type) {
		this.literal = value;
		this.typedVar = new TypedVar(type);
		if (literal != 0 && type.isObject())
			throw new RuntimeException("wrong literal type");
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
