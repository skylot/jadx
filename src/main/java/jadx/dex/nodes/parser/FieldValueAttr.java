package jadx.dex.nodes.parser;

import jadx.dex.attributes.AttributeType;
import jadx.dex.attributes.IAttribute;

public class FieldValueAttr implements IAttribute {

	private final Object value;

	public FieldValueAttr(Object value) {
		this.value = value;
	}

	@Override
	public AttributeType getType() {
		return AttributeType.FIELD_VALUE;
	}

	public Object getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "V=" + value;
	}
}
