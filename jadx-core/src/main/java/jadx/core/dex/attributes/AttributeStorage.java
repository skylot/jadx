package jadx.core.dex.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.core.dex.attributes.annotations.AnnotationsList;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Storage for different attribute types:
 * 1. flags - boolean attribute (set or not)
 * 2. attribute - class instance associated with attribute type.
 */
public class AttributeStorage {

	static {
		int flagsCount = AFlag.values().length;
		if (flagsCount >= 64) {
			throw new JadxRuntimeException("Try to reduce flags count to 64 for use one long in EnumSet, now " + flagsCount);
		}
	}

	private final Set<AFlag> flags;
	private Map<AType<?>, IAttribute> attributes;

	public AttributeStorage() {
		flags = EnumSet.noneOf(AFlag.class);
		attributes = Collections.emptyMap();
	}

	public void add(AFlag flag) {
		flags.add(flag);
	}

	public void add(IAttribute attr) {
		writeAttributes().put(attr.getType(), attr);
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
		writeAttributes().putAll(otherList.attributes);
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

	public IAnnotation getAnnotation(String cls) {
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
		if (!attributes.isEmpty()) {
			attributes.remove(type);
		}
	}

	public void remove(IAttribute attr) {
		if (!attributes.isEmpty()) {
			AType<? extends IAttribute> type = attr.getType();
			IAttribute a = attributes.get(type);
			if (a == attr) {
				attributes.remove(type);
			}
		}
	}

	private Map<AType<?>, IAttribute> writeAttributes() {
		if (attributes.isEmpty()) {
			attributes = new IdentityHashMap<>(5);
		}
		return attributes;
	}

	public void clear() {
		flags.clear();
		if (!attributes.isEmpty()) {
			attributes.clear();
		}
	}

	public synchronized void unloadAttributes() {
		if (attributes.isEmpty()) {
			return;
		}
		Set<AType<?>> skipOnUnload = AType.SKIP_ON_UNLOAD;
		attributes.keySet().removeIf(attrType -> !skipOnUnload.contains(attrType));
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
			list.add(a.toAttrString());
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
		list.sort(String::compareTo);
		return "A[" + Utils.listToString(list) + ']';
	}
}
