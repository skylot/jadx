package jadx.plugins.input.java.data.attributes.types;

import java.util.List;

import jadx.api.plugins.input.data.attributes.types.ExceptionsAttr;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class JavaExceptionsAttr extends ExceptionsAttr implements IJavaAttribute {
	public JavaExceptionsAttr(List<String> list) {
		super(list);
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new JavaExceptionsAttr(reader.readClassesList(clsData.getConstPoolReader()));
	}
}
