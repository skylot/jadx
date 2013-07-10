package jadx.core.dex.attributes.annotations;

import jadx.core.dex.attributes.AttributeType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.utils.Utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationsList implements IAttribute {

	private final Map<String, Annotation> map;

	public AnnotationsList(List<Annotation> anList) {
		map = new HashMap<String, Annotation>(anList.size());
		for (Annotation a : anList) {
			map.put(a.getAnnotationClass(), a);
		}
	}

	public Annotation get(String className) {
		return map.get(className);
	}

	public Collection<Annotation> getAll() {
		return map.values();
	}

	public int size() {
		return map.size();
	}

	@Override
	public AttributeType getType() {
		return AttributeType.ANNOTATION_LIST;
	}

	@Override
	public String toString() {
		return Utils.listToString(map.values());
	}

}
