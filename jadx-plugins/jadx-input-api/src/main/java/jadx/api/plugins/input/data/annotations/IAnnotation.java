package jadx.api.plugins.input.data.annotations;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

public interface IAnnotation {
	String getAnnotationClass();

	AnnotationVisibility getVisibility();

	Map<String, EncodedValue> getValues();

	@Nullable
	default EncodedValue getDefaultValue() {
		return getValues().get("value");
	}
}
