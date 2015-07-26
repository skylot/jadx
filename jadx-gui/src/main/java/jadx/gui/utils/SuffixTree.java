package jadx.gui.utils;

import com.googlecode.concurrenttrees.radix.node.concrete.DefaultCharArrayNodeFactory;
import com.googlecode.concurrenttrees.suffix.ConcurrentSuffixTree;

public class SuffixTree<V> {

	private final ConcurrentSuffixTree<V> tree;

	public SuffixTree() {
		this.tree = new ConcurrentSuffixTree<V>(new DefaultCharArrayNodeFactory());
	}

	public void put(String str, V value) {
		if (str == null || str.isEmpty()) {
			return;
		}
		tree.putIfAbsent(str, value);
	}

	public Iterable<V> getValuesForKeysContaining(String str) {
		return tree.getValuesForKeysContaining(str);
	}

	public int size() {
		return tree.size();
	}
}
