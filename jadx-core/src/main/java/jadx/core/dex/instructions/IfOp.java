package jadx.core.dex.instructions;

import jadx.core.utils.exceptions.JadxRuntimeException;

public enum IfOp {
	EQ("=="),
	NE("!="),
	LT("<"),
	LE("<="),
	GT(">"),
	GE(">=");

	private final String symbol;

	IfOp(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public IfOp invert() {
		switch (this) {
			case EQ:
				return NE;
			case NE:
				return EQ;

			case LT:
				return GE;
			case LE:
				return GT;

			case GT:
				return LE;
			case GE:
				return LT;

			default:
				throw new JadxRuntimeException("Unknown if operations type: " + this);
		}
	}
}
