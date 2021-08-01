package jadx.plugins.input.java.data.attributes.types;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.types.AnnotationDefaultAttr;
import jadx.plugins.input.java.data.attributes.EncodedValueReader;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;
import jadx.plugins.input.java.data.attributes.JavaAttrStorage;
import jadx.plugins.input.java.data.attributes.JavaAttrType;

public class JavaAnnotationDefaultAttr extends AnnotationDefaultAttr implements IJavaAttribute {

	public JavaAnnotationDefaultAttr(EncodedValue value) {
		super(value);
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new JavaAnnotationDefaultAttr(EncodedValueReader.read(clsData, reader));
	}

	public static AnnotationDefaultAttr convert(JavaAttrStorage attributes) {
		return attributes.get(JavaAttrType.ANNOTATION_DEFAULT);
	}
}
