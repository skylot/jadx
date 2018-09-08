package jadx.core.dex.attributes.annotations;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.utils.Utils;

public class AnnotationsList implements IAttribute {

	public static final AnnotationsList EMPTY = new AnnotationsList(Collections.<Annotation>emptyList());

	private final Map<String, Annotation> map;

	public AnnotationsList(List<Annotation> anList) {
		map = new HashMap<>(anList.size());
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

	public boolean isEmpty() {
		return map.isEmpty();
	}

	@Override
	public AType<AnnotationsList> getType() {
		return AType.ANNOTATION_LIST;
	}

	@Override
	public String toString() {
		return Utils.listToString(map.values());
	}
}
