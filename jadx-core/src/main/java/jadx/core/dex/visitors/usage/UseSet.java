package jadx.core.dex.visitors.usage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

public class UseSet<K, V> {
	private final Map<K, Set<V>> useMap = new HashMap<>();

	public void add(K obj, V use) {
		if (obj == use) {
			// self excluded
			return;
		}
		Set<V> set = useMap.computeIfAbsent(obj, k -> new HashSet<>());
		set.add(use);
	}

	public Set<V> get(K obj) {
		return useMap.get(obj);
	}

	public void visit(BiConsumer<K, Set<V>> consumer) {
		for (Map.Entry<K, Set<V>> entry : useMap.entrySet()) {
			consumer.accept(entry.getKey(), entry.getValue());
		}
	}
}
