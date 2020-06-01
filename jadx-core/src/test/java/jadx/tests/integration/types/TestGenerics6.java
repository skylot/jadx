package jadx.tests.integration.types;

import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenerics6 extends IntegrationTest {

	public static class TestCls<K, V> implements Iterable<Map.Entry<K, V>> {
		public V test(K key, V v) {
			Entry<K, V> entry = get(key);
			if (entry != null) {
				return entry.mValue;
			}
			put(key, v);
			return null;
		}

		protected Entry<K, V> get(K k) {
			return null;
		}

		protected Entry<K, V> put(K key, V v) {
			return null;
		}

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return null;
		}

		static class Entry<K, V> implements Map.Entry<K, V> {
			final V mValue;

			Entry(K key, V value) {
				this.mValue = value;
			}

			@Override
			public K getKey() {
				return null;
			}

			@Override
			public V getValue() {
				return null;
			}

			@Override
			public V setValue(V value) {
				return null;
			}
		}

	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("Entry entry = get(k);")
				.containsOne("Entry<K, V> entry = get(k);");
	}
}
