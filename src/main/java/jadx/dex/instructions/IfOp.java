package jadx.dex.instructions;

import jadx.utils.exceptions.JadxRuntimeException;

public enum IfOp {
	EQ("=="),
	NE("!="),
	LT("<"),
	LE("<="),
	GT(">"),
	GE(">=");

	private final String symbol;

	private IfOp(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return symbol;
	}

	public IfOp invert() {
		switch (this) {
			case EQ:
				return IfOp.NE;
			case NE:
				return IfOp.EQ;

			case LT:
				return IfOp.GE;
			case LE:
				return IfOp.GT;

			case GT:
				return IfOp.LE;
			case GE:
				return IfOp.LT;

			default:
				throw new JadxRuntimeException("Unknown if operations type: " + this);
		}
	}
}
