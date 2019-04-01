package jadx.tests.integration.generics;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Map;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestGenerics2 extends IntegrationTest {

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public ItemReference(V item, Object objId, ReferenceQueue<? super V> queue) {"));
		assertThat(code, containsString("public V get(Object id) {"));
		assertThat(code, containsString("WeakReference<V> ref = "));
		assertThat(code, containsString("return ref.get();"));
	}

	@Test
	@NotYetImplemented("Make generic info propagation for methods (like Map.get)")
	public void testDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("WeakReference<V> ref = "));
	}
}
