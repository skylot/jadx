package jadx.plugins.input.dex.sections.annotations;

import jadx.api.plugins.input.data.annotations.EncodedType;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;

public class AnnotationsUtils {

	@SuppressWarnings("unchecked")
	public static <T> T getValue(IAnnotation ann, String name, EncodedType type, T defValue) {
		if (ann == null || ann.getValues() == null || ann.getValues().isEmpty()) {
			return defValue;
		}
		EncodedValue encodedValue = ann.getValues().get(name);
		if (encodedValue == null || encodedValue.getType() != type) {
			return defValue;
		}
		return (T) encodedValue.getValue();
	}
}
