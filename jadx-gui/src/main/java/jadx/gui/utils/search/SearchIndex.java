package jadx.gui.utils.search;

import io.reactivex.Flowable;

public interface SearchIndex<V> {

	void put(String str, V value);

	Flowable<V> search(String searchStr, boolean caseInsensitive);

	int size();
}
