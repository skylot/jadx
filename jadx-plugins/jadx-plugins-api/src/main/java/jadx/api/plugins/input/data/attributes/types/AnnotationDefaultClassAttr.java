package jadx.api.plugins.input.data.attributes.types;

import java.util.Map;

import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.PinnedAttribute;

public class AnnotationDefaultClassAttr extends PinnedAttribute {

	private final Map<String, EncodedValue> values;

	public AnnotationDefaultClassAttr(Map<String, EncodedValue> values) {
		this.values = values;
	}

	public Map<String, EncodedValue> getValues() {
		return values;
	}

	@Override
	public IJadxAttrType<? extends IJadxAttribute> getAttrType() {
		return JadxAttrType.ANNOTATION_DEFAULT_CLASS;
	}

	@Override
	public String toString() {
		return "ANNOTATION_DEFAULT_CLASS: " + values;
	}
}
