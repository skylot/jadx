package jadx.gui.utils.search;

import java.util.List;

public abstract class SearchIndex<V> {

	public abstract void put(String str, V value);

	public void put(StringRef str, V value) {
		throw new UnsupportedOperationException("StringRef put not supported");
	}

	public boolean isStringRefSupported() {
		return false;
	}

	public abstract List<V> getValuesForKeysContaining(String str);

	public abstract int size();
}
