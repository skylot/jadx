package jadx.api.plugins.input.data.annotations;

public class EncodedValue {
	public static final EncodedValue NULL = new EncodedValue(EncodedType.ENCODED_NULL, new Object());

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
	public String toString() {
		switch (type) {
			case ENCODED_STRING:
				return (String) value;
			case ENCODED_ARRAY:
				return "[" + value + "]";
			default:
				return "{" + type + ": " + value + '}';
		}
	}
}
