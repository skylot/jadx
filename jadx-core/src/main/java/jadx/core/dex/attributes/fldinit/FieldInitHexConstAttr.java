package jadx.core.dex.attributes.fldinit;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;

public final class FieldInitHexConstAttr extends FieldInitAttr {
	private final EncodedValue value;

	FieldInitHexConstAttr(Object val) {
		value = new EncodedValue(EncodedType.ENCODED_HEX_NUMBER, val);
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
		return "INIT_HEX{" + value + '}';
	}
}
