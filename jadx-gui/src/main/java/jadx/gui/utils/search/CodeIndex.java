package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CodeIndex<T> extends SearchIndex<T> {

	private final List<StringRef> keys = new ArrayList<StringRef>();
	private final List<T> values = new ArrayList<T>();

	@Override
	public void put(String str, T value) {
		throw new UnsupportedOperationException("CodeIndex.put for string not supported");
	}

	@Override
	public void put(StringRef str, T value) {
		if (str == null || str.length() == 0) {
			return;
		}
		keys.add(str);
		values.add(value);
	}

	@Override
	public boolean isStringRefSupported() {
		return true;
	}

	@Override
	public List<T> getValuesForKeysContaining(String str) {
		int size = size();
		if (size == 0) {
			return Collections.emptyList();
		}
		List<T> results = new ArrayList<T>();
		for (int i = 0; i < size; i++) {
			StringRef key = keys.get(i);
			if (key.indexOf(str) != -1) {
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
