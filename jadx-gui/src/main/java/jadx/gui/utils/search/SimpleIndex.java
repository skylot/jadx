package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SimpleIndex<T> implements SearchIndex<T> {

	private final List<String> keys = new ArrayList<>();
	private final List<T> values = new ArrayList<>();

	@Override
	public void put(String str, T value) {
		keys.add(str);
		values.add(value);
	}

	@Override
	public void put(StringRef str, T value) {
		throw new UnsupportedOperationException("StringRef not supported");
	}

	@Override
	public boolean isStringRefSupported() {
		return false;
	}

	@Override
	public List<T> getValuesForKeysContaining(String str, boolean caseInsensitive) {
		int size = size();
		if (size == 0) {
			return Collections.emptyList();
		}
		if (caseInsensitive) {
			str = str.toLowerCase();
		}
		List<T> results = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			String key = keys.get(i);
			if (caseInsensitive) {
				key = key.toLowerCase();
			}
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
