package jadx.api.args;

public enum IntegerFormat {
	AUTO,
	DECIMAL,
	HEXADECIMAL;

	public boolean isHexadecimal() {
		return this == HEXADECIMAL;
	}
}
