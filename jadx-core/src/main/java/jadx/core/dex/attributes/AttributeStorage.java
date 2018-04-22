package jadx.core.dex.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.core.dex.attributes.annotations.Annotation;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.utils.Utils;

/**
 * Storage for different attribute types:
 * 1. flags - boolean attribute (set or not)
 * 2. attribute - class instance associated with attribute type.
 */
public class AttributeStorage {

	private final Set<AFlag> flags;
	private final Map<AType<?>, IAttribute> attributes;

	public AttributeStorage() {
		flags = EnumSet.noneOf(AFlag.class);
		attributes = new IdentityHashMap<>();
	}

	public void add(AFlag flag) {
		flags.add(flag);
	}

	public void add(IAttribute attr) {
		attributes.put(attr.getType(), attr);
	}

	public <T> void add(AType<AttrList<T>> type, T obj) {
		AttrList<T> list = get(type);
		if (list == null) {
			list = new AttrList<>(type);
			add(list);
		}
		list.getList().add(obj);
	}

	public void addAll(AttributeStorage otherList) {
		flags.addAll(otherList.flags);
		attributes.putAll(otherList.attributes);
	}

	public boolean contains(AFlag flag) {
		return flags.contains(flag);
	}

	public <T extends IAttribute> boolean contains(AType<T> type) {
		return attributes.containsKey(type);
	}

	@SuppressWarnings("unchecked")
	public <T extends IAttribute> T get(AType<T> type) {
		return (T) attributes.get(type);
	}

	public Annotation getAnnotation(String cls) {
		AnnotationsList aList = get(AType.ANNOTATION_LIST);
		return aList == null ? null : aList.get(cls);
	}

	public <T> List<T> getAll(AType<AttrList<T>> type) {
		AttrList<T> attrList = get(type);
		if (attrList == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(attrList.getList());
	}

	public void remove(AFlag flag) {
		flags.remove(flag);
	}

	public <T extends IAttribute> void remove(AType<T> type) {
		attributes.remove(type);
	}

	public void remove(IAttribute attr) {
		AType<? extends IAttribute> type = attr.getType();
		IAttribute a = attributes.get(type);
		if (a == attr) {
			attributes.remove(type);
		}
	}

	public void clear() {
		flags.clear();
		attributes.clear();
	}

	public List<String> getAttributeStrings() {
		int size = flags.size() + attributes.size() + attributes.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>(size);
		for (AFlag a : flags) {
			list.add(a.toString());
		}
		for (IAttribute a : attributes.values()) {
			list.add(a.toString());
		}
		return list;
	}

	public boolean isEmpty() {
		return flags.isEmpty() && attributes.isEmpty();
	}

	@Override
	public String toString() {
		List<String> list = getAttributeStrings();
		if (list.isEmpty()) {
			return "";
		}
		return "A:{" + Utils.listToString(list) + "}";
	}
}
