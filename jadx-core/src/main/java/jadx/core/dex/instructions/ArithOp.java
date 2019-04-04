package jadx.core.dex.instructions;

public enum ArithOp {
	ADD("+"),
	SUB("-"),
	MUL("*"),
	DIV("/"),
	REM("%"),

	AND("&"),
	OR("|"),
	XOR("^"),

	SHL("<<"),
	SHR(">>"),
	USHR(">>>");

	private final String symbol;

	ArithOp(String symbol) {
		this.symbol = symbol;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public boolean noWrapWith(ArithOp other) {
		return (this == ADD && other == ADD)
				|| (this == MUL && other == MUL)
				|| (this == AND && other == AND)
				|| (this == OR && other == OR);
	}
}
