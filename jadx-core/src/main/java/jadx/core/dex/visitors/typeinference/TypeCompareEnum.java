package jadx.core.dex.visitors.typeinference;

public enum TypeCompareEnum {
	EQUAL,
	NARROW,
	NARROW_BY_GENERIC, // same basic type with generic
	WIDER,
	WIDER_BY_GENERIC, // same basic type without generic
	CONFLICT,
	UNKNOWN;

	public TypeCompareEnum invert() {
		switch (this) {
			case NARROW:
				return WIDER;

			case NARROW_BY_GENERIC:
				return WIDER_BY_GENERIC;

			case WIDER:
				return NARROW;

			case WIDER_BY_GENERIC:
				return NARROW_BY_GENERIC;

			case CONFLICT:
			case EQUAL:
			case UNKNOWN:
			default:
				return this;
		}
	}

	public boolean isEqual() {
		return this == EQUAL;
	}

	public boolean isWider() {
		return this == WIDER || this == WIDER_BY_GENERIC;
	}

	public boolean isWiderOrEqual() {
		return isEqual() || isWider();
	}

	public boolean isNarrow() {
		return this == NARROW || this == NARROW_BY_GENERIC;
	}

	public boolean isNarrowOrEqual() {
		return isEqual() || isNarrow();
	}

	public boolean isGeneric() {
		return this == WIDER_BY_GENERIC || this == NARROW_BY_GENERIC;
	}
}
