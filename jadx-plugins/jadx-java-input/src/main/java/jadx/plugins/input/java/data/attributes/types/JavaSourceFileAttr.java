package jadx.plugins.input.java.data.attributes.types;

import jadx.api.plugins.input.data.attributes.types.SourceFileAttr;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class JavaSourceFileAttr extends SourceFileAttr implements IJavaAttribute {

	public JavaSourceFileAttr(String fileName) {
		super(fileName);
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new JavaSourceFileAttr(clsData.getConstPoolReader().getUtf8(reader.readU2()));
	}
}
