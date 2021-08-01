package jadx.plugins.input.java.data.attributes.types;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class ConstValueAttr implements IJavaAttribute {

	private final EncodedValue value;

	public ConstValueAttr(EncodedValue value) {
		this.value = value;
	}

	public EncodedValue getValue() {
		return value;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new ConstValueAttr(clsData.getConstPoolReader().readAsEncodedValue(reader.readU2()));
	}
}
