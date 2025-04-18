package jadx.gui.utils.cache;

import java.util.function.Function;

/**
 * Simple store for values depending on 'key' object.
 *
 * @param <K> key object type
 * @param <V> stored object type
 */
public class ValueCache<K, V> {
	private K key;
	private V value;

	/**
	 * Return a stored object if key not changed, load a new object overwise.
	 */
	public synchronized V get(K requestKey, Function<K, V> loadFunc) {
		if (key != null && key.equals(requestKey)) {
			return value;
		}
		V newValue = loadFunc.apply(requestKey);
		key = requestKey;
		value = newValue;
		return newValue;
	}
}
