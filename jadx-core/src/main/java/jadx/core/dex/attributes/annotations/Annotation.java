package jadx.core.dex.attributes.annotations;

import java.util.Map;

import jadx.core.dex.instructions.args.ArgType;

public class Annotation {

	public enum Visibility {
		BUILD, RUNTIME, SYSTEM
	}

	private final Visibility visibility;
	private final ArgType atype;
	private final Map<String, Object> values;

	public Annotation(Visibility visibility, ArgType type, Map<String, Object> values) {
		this.visibility = visibility;
		this.atype = type;
		this.values = values;
	}

	public Visibility getVisibility() {
		return visibility;
	}

	public ArgType getType() {
		return atype;
	}

	public String getAnnotationClass() {
		return atype.getObject();
	}

	public Map<String, Object> getValues() {
		return values;
	}

	public Object getDefaultValue() {
		return values.get("value");
	}

	@Override
	public String toString() {
		return "Annotation[" + visibility + ", " + atype + ", " + values + "]";
	}

}
