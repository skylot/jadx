package jadx.plugins.input.dex.sections.annotations;

import java.util.Map;

import jadx.api.plugins.input.data.annotations.AnnotationVisibility;
import jadx.api.plugins.input.data.annotations.EncodedValue;
import jadx.api.plugins.input.data.annotations.IAnnotation;

public class DexAnnotation implements IAnnotation {
	private final AnnotationVisibility visibility;
	private final String type;
	private final Map<String, EncodedValue> values;

	public DexAnnotation(AnnotationVisibility visibility, String type, Map<String, EncodedValue> values) {
		this.visibility = visibility;
		this.type = type;
		this.values = values;
	}

	@Override
	public String getAnnotationClass() {
		return type;
	}

	@Override
	public AnnotationVisibility getVisibility() {
		return visibility;
	}

	@Override
	public Map<String, EncodedValue> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return "DexAnnotation{" + visibility + ", type=" + type + ", values=" + values + '}';
	}
}
