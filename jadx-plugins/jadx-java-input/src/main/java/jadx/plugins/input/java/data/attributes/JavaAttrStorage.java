package jadx.plugins.input.java.data.attributes;

import org.jetbrains.annotations.Nullable;

public class JavaAttrStorage {
	public static final JavaAttrStorage EMPTY = new JavaAttrStorage();

	private final IJavaAttribute[] map = new IJavaAttribute[JavaAttrType.size()];

	public void add(JavaAttrType<?> type, IJavaAttribute value) {
		map[type.getId()] = value;
	}

	@SuppressWarnings("unchecked")
	@Nullable
	public <A extends IJavaAttribute> A get(JavaAttrType<A> type) {
		return (A) map[type.getId()];
	}

	public int size() {
		int size = 0;
		for (IJavaAttribute attr : map) {
			if (attr != null) {
				size++;
			}
		}
		return size;
	}

	@Override
	public String toString() {
		return "AttributesStorage{size=" + size() + '}';
	}
}
