package jadx.plugins.input.java.data.attributes.types;

import jadx.api.plugins.input.data.attributes.types.SignatureAttr;
import jadx.plugins.input.java.data.attributes.IJavaAttribute;
import jadx.plugins.input.java.data.attributes.IJavaAttributeReader;

public class JavaSignatureAttr extends SignatureAttr implements IJavaAttribute {

	public JavaSignatureAttr(String signature) {
		super(signature);
	}

	public static IJavaAttributeReader reader() {
		return (clsData, reader) -> new JavaSignatureAttr(clsData.getConstPoolReader().getUtf8(reader.readU2()));
	}
}
