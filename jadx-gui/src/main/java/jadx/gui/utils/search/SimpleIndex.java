package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleIndex<T> extends SearchIndex<T> {

	private final List<String> keys = new ArrayList<String>();
	private final List<T> values = new ArrayList<T>();

	@Override
	public void put(String str, T value) {
		keys.add(str);
		values.add(value);
	}

	@Override
	public List<T> getValuesForKeysContaining(String str) {
		int size = size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<T> results = new ArrayList<T>();
		for (int i = 0; i < size; i++) {
			String key = keys.get(i);
			if (key.contains(str)) {
				results.add(values.get(i));
			}
		}
		return results;
	}

	@Override
	public int size() {
		return keys.size();
	}
}
