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

}
