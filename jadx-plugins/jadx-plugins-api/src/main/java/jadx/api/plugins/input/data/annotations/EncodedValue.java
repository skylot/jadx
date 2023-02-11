package jadx.api.plugins.input.data.annotations;

import java.util.Objects;

import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class EncodedValue extends PinnedAttribute {
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
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return JadxAttrType.CONSTANT_VALUE;
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
			case ENCODED_ARRAY:
				return "[" + value + "]";
			case ENCODED_STRING:
				return "{STRING: \"" + value + "\"}";
			default:
				return "{" + type.toString().substring(8) + ": " + value + '}';
		}
	}
}
