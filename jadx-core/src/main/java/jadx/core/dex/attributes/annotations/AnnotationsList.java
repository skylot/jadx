package jadx.core.dex.attributes.annotations;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.dex.attributes.AType;
import jadx.core.dex.attributes.IAttribute;
import jadx.core.dex.nodes.ICodeNode;
import jadx.core.utils.Utils;

public class AnnotationsList implements IAttribute {

	public static void attach(ICodeNode node, List<IAnnotation> annotationList) {
		AnnotationsList attrList = pack(annotationList);
		if (attrList != null) {
			node.addAttr(attrList);
		}
	}

	@Nullable
	public static AnnotationsList pack(List<IAnnotation> annotationList) {
		if (annotationList.isEmpty()) {
			return null;
		}
		Map<String, IAnnotation> annMap = new HashMap<>(annotationList.size());
		for (IAnnotation ann : annotationList) {
			annMap.put(ann.getAnnotationClass(), ann);
		}
		return new AnnotationsList(annMap);
	}

	private final Map<String, IAnnotation> map;

	public AnnotationsList(Map<String, IAnnotation> map) {
		this.map = map;
	}

	public IAnnotation get(String className) {
		return map.get(className);
	}

	public Collection<IAnnotation> getAll() {
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
