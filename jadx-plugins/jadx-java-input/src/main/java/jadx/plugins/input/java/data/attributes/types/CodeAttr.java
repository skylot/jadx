package jadx.plugins.input.java.data.attributes.types;

import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class CodeAttr implements IJavaAttribute {
	private final int offset;

	public CodeAttr(int offset) {
		this.offset = offset;
	}

	public int getOffset() {
		return offset;
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new CodeAttr(reader.getOffset());
	}
}
