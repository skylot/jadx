package jadx.dex.instructions;

public enum ArithOp {
	ADD("+"),
	SUB("-"),
	MUL("*"),
	DIV("/"),
	REM("%"),

	INC("++"),
	DEC("--"),

	AND("&"),
	OR("|"),
	XOR("^"),

	SHL("<<"),
	SHR(">>"),
	USHR(">>>");

	private ArithOp(String symbol) {
		this.symbol = symbol;
	}

	private final String symbol;

	public String getSymbol() {
		return this.symbol;
	}

}
