package jadx.core.dex.instructions.args;

public enum PrimitiveType {
	BOOLEAN("Z", "boolean", ArgType.object("java.lang.Boolean")),
	CHAR("C", "char", ArgType.object("java.lang.Character")),
	BYTE("B", "byte", ArgType.object("java.lang.Byte")),
	SHORT("S", "short", ArgType.object("java.lang.Short")),
	INT("I", "int", ArgType.object("java.lang.Integer")),
	FLOAT("F", "float", ArgType.object("java.lang.Float")),
	LONG("J", "long", ArgType.object("java.lang.Long")),
	DOUBLE("D", "double", ArgType.object("java.lang.Double")),
	OBJECT("L", "OBJECT", ArgType.OBJECT),
	ARRAY("[", "ARRAY", ArgType.OBJECT_ARRAY),
	VOID("V", "void", ArgType.object("java.lang.Void"));

	private final String shortName;
	private final String longName;
	private final ArgType boxType;

	PrimitiveType(String shortName, String longName, ArgType boxType) {
		this.shortName = shortName;
		this.longName = longName;
		this.boxType = boxType;
	}

	public String getShortName() {
		return shortName;
	}

	public String getLongName() {
		return longName;
	}

	public ArgType getBoxType() {
		return boxType;
	}

	@Override
	public String toString() {
		return longName;
	}

	public boolean isObjectOrArray() {
		return this == OBJECT || this == ARRAY;
	}
}
