package jadx.gui.utils.search;

import java.util.List;

public interface SearchIndex<V> {

	void put(String str, V value);

	void put(StringRef str, V value);

	boolean isStringRefSupported();

	List<V> getValuesForKeysContaining(String str, boolean caseInsensitive);

	int size();
}
