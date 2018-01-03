package jadx.tests.integration.generics;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestGenerics2 extends IntegrationTest {

	public static class TestCls {
		private static class ItemReference<V> extends WeakReference<V> {
			private Object id;

			public ItemReference(V item, Object id, ReferenceQueue<? super V> queue) {
				super(item, queue);
				this.id = id;
			}
		}

		public static class ItemReferences<V> {
			private Map<Object, ItemReference<V>> items;

			public V get(Object id) {
				WeakReference<V> ref = this.items.get(id);
				return (ref != null) ? ref.get() : null;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public ItemReference(V item, Object id, ReferenceQueue<? super V> queue) {"));
		assertThat(code, containsString("public V get(Object id) {"));
		assertThat(code, containsString("WeakReference<V> ref = "));
		assertThat(code, containsString("return ref != null ? ref.get() : null;"));
	}
}
