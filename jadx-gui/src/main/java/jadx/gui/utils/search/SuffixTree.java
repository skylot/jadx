package jadx.gui.utils.search;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;

public class SuffixTree<V> extends SearchIndex<V> {

	private final ConcurrentSuffixTree<V> tree;

	public SuffixTree() {
		this.tree = new ConcurrentSuffixTree<V>(new DefaultCharArrayNodeFactory());
	}

	@Override
	public void put(String str, V value) {
		if (str == null || str.isEmpty()) {
			return;
		}
		tree.putIfAbsent(str, value);
	}

	@Override
	public List<V> getValuesForKeysContaining(String str) {
		Iterable<V> resultsIt = tree.getValuesForKeysContaining(str);
		List<V> list = new ArrayList<V>();
		for (V v : resultsIt) {
			list.add(v);
		}
		return list;
	}

	@Override
	public int size() {
		return tree.size();
	}
}
