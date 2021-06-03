package jadx.tests.integration.generics;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestGenerics2 extends IntegrationTest {

	@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
	public static class TestCls {
		public static class ItemReference<V> extends WeakReference<V> {
			public Object id;

			public ItemReference(V item, Object objId, ReferenceQueue<? super V> queue) {
				super(item, queue);
				this.id = objId;
			}
		}

		public static class ItemReferences<V> {
			private Map<Object, ItemReference<V>> items;

			public V get(Object id) {
				WeakReference<V> ref = this.items.get(id);
				if (ref != null) {
					return ref.get();
				}
				return null;
			}
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("public ItemReference(V item, Object objId, ReferenceQueue<? super V> queue) {")
				.containsOne("public V get(Object id) {")
				.containsOne("WeakReference<V> ref = ")
				.containsOne("return ref.get();");
	}

	@Test
	public void testDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ItemReference<V> itemReference = this.items.get(obj);")
				.containsOne("return itemReference.get();");
	}
}
