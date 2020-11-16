package jadx.core.dex.attributes.fldinit;

import jadx.api.plugins.input.data.annotations.EncodedValue;

import static java.util.Objects.requireNonNull;

public final class FieldInitConstAttr extends FieldInitAttr {
	public static final FieldInitAttr NULL_VALUE = new FieldInitConstAttr(EncodedValue.NULL);

	private final EncodedValue value;

	FieldInitConstAttr(EncodedValue value) {
		this.value = requireNonNull(value);
	}

	@Override
	public EncodedValue getEncodedValue() {
		return value;
	}

	@Override
	public boolean isConst() {
		return true;
	}

	@Override
	public String toString() {
		return "INIT{" + value + '}';
	}
}
