package jadx.api.plugins.input.data.annotations;

import java.util.Objects;

public class EncodedValue {
	public static final EncodedValue NULL = new EncodedValue(EncodedType.ENCODED_NULL, null);

	private final EncodedType type;
	private final Object value;

	public EncodedValue(EncodedType type, Object value) {
		this.type = type;
		this.value = value;
	}

	public EncodedType getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		EncodedValue that = (EncodedValue) o;
		return type == that.getType() && Objects.equals(value, that.getValue());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getType(), getValue());
	}

	@Override
	public String toString() {
		switch (type) {
			case ENCODED_NULL:
				return "null";
			case ENCODED_STRING:
				return (String) value;
			case ENCODED_ARRAY:
				return "[" + value + "]";
			default:
				return "{" + type + ": " + value + '}';
		}
	}
}
