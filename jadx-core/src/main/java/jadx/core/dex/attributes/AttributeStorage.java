package jadx.core.dex.attributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import jadx.api.plugins.input.data.annotations.IAnnotation;
import jadx.api.plugins.input.data.attributes.IJadxAttrType;
import jadx.api.plugins.input.data.attributes.IJadxAttribute;
import jadx.api.plugins.input.data.attributes.JadxAttrType;
import jadx.api.plugins.input.data.attributes.types.AnnotationsAttr;
import jadx.core.utils.Utils;
import jadx.core.utils.exceptions.JadxRuntimeException;

/**
 * Storage for different attribute types:<br>
 * 1. Flags - boolean attribute (set or not)<br>
 * 2. Attributes - class instance ({@link IJadxAttribute}) associated with an attribute type
 * ({@link IJadxAttrType})<br>
 */
public class AttributeStorage {

	public static AttributeStorage fromList(List<IJadxAttribute> list) {
		AttributeStorage storage = new AttributeStorage();
		storage.add(list);
		return storage;
	}

	static {
		int flagsCount = AFlag.values().length;
		if (flagsCount >= 64) {
			throw new JadxRuntimeException("Try to reduce flags count to 64 for use one long in EnumSet, now " + flagsCount);
		}
	}

	private static final Map<IJadxAttrType<?>, IJadxAttribute> EMPTY_ATTRIBUTES = Collections.emptyMap();

	private final Set<AFlag> flags;
	private Map<IJadxAttrType<?>, IJadxAttribute> attributes;

	public AttributeStorage() {
		flags = EnumSet.noneOf(AFlag.class);
		attributes = EMPTY_ATTRIBUTES;
	}

	public void add(AFlag flag) {
		flags.add(flag);
	}

	public void add(IJadxAttribute attr) {
		writeAttributes(map -> map.put(attr.getAttrType(), attr));
	}

	public void add(List<IJadxAttribute> list) {
		writeAttributes(map -> list.forEach(attr -> map.put(attr.getAttrType(), attr)));
	}

	public <T> void add(IJadxAttrType<AttrList<T>> type, T obj) {
		AttrList<T> list = get(type);
		if (list == null) {
			list = new AttrList<>(type);
			add(list);
		}
		list.getList().add(obj);
	}

	public void addAll(AttributeStorage otherList) {
		flags.addAll(otherList.flags);
		if (!otherList.attributes.isEmpty()) {
			writeAttributes(m -> m.putAll(otherList.attributes));
		}
	}

	public boolean contains(AFlag flag) {
		return flags.contains(flag);
	}

	public <T extends IJadxAttribute> boolean contains(IJadxAttrType<T> type) {
		return attributes.containsKey(type);
	}

	@SuppressWarnings("unchecked")
	public <T extends IJadxAttribute> T get(IJadxAttrType<T> type) {
		return (T) attributes.get(type);
	}

	public IAnnotation getAnnotation(String cls) {
		AnnotationsAttr aList = get(JadxAttrType.ANNOTATION_LIST);
		return aList == null ? null : aList.get(cls);
	}

	public <T> List<T> getAll(IJadxAttrType<AttrList<T>> type) {
		AttrList<T> attrList = get(type);
		if (attrList == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(attrList.getList());
	}

	public void remove(AFlag flag) {
		flags.remove(flag);
	}

	public void clearFlags() {
		flags.clear();
	}

	public <T extends IJadxAttribute> void remove(IJadxAttrType<T> type) {
		if (!attributes.isEmpty()) {
			writeAttributes(map -> map.remove(type));
		}
	}

	public void remove(IJadxAttribute attr) {
		if (!attributes.isEmpty()) {
			writeAttributes(map -> {
				IJadxAttrType<? extends IJadxAttribute> type = attr.getAttrType();
				IJadxAttribute a = map.get(type);
				if (a == attr) {
					map.remove(type);
				}
			});
		}
	}

	private void writeAttributes(Consumer<Map<IJadxAttrType<?>, IJadxAttribute>> mapConsumer) {
		synchronized (this) {
			if (attributes == EMPTY_ATTRIBUTES) {
				attributes = new IdentityHashMap<>(2); // only 1 or 2 attributes added in most cases
			}
			mapConsumer.accept(attributes);
			if (attributes.isEmpty()) {
				attributes = EMPTY_ATTRIBUTES;
			}
		}
	}

	public void unloadAttributes() {
		if (attributes.isEmpty()) {
			return;
		}
		writeAttributes(map -> map.entrySet().removeIf(entry -> !entry.getValue().keepLoaded()));
	}

	public List<String> getAttributeStrings() {
		int size = flags.size() + attributes.size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<String> list = new ArrayList<>(size);
		for (AFlag a : flags) {
			list.add(a.toString());
		}
		for (IJadxAttribute a : attributes.values()) {
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
