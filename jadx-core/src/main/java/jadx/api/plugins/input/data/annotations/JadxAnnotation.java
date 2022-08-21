package jadx.api.plugins.input.data.annotations;

import java.util.Map;

public class JadxAnnotation implements IAnnotation {
	private final AnnotationVisibility visibility;
	private final String type;
	private final Map<String, EncodedValue> values;

	public JadxAnnotation(AnnotationVisibility visibility, String type, Map<String, EncodedValue> values) {
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
		return "Annotation{" + visibility + ", type=" + type + ", values=" + values + '}';
	}
}
