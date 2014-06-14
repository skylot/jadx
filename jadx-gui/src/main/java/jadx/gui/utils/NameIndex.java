package jadx.gui.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NameIndex<T> {

	private final List<String> strings = new ArrayList<String>();
	private final List<T> objects = new ArrayList<T>();

	public void add(String name, T obj) {
		strings.add(name);
		objects.add(obj);
	}

	public List<T> search(String text) {
		List<T> results = new ArrayList<T>();
		int count = strings.size();
		for (int i = 0; i < count; i++) {
			String name = strings.get(i);
			if (name.contains(text)) {
				results.add(objects.get(i));
			}
		}
		return results.isEmpty() ? Collections.<T>emptyList() : results;
	}
}
