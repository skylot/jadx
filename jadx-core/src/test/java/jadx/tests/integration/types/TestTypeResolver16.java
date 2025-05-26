package jadx.tests.integration.types;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static java.util.Collections.emptyList;

/**
 * Issue 1002
 * Insertion of additional cast (at use place) needed for successful type inference
 */
public class TestTypeResolver16 extends SmaliTest {

	@SuppressWarnings("unchecked")
	public static class TestCls {

		public final <T, K> List<T> test(List<? extends T> list,
				Set<? extends T> set, Function<? super T, ? extends K> function) {
			if (set != null) {
				List<? extends T> union = list != null ? union(list, set, function) : null;
				if (union != null) {
					list = union;
				}
			}
			return list != null ? (List<T>) list : emptyList();
		}

		public static <T, K> List<T> union(
				Collection<? extends T> collection,
				Iterable<? extends T> iterable,
				Function<? super T, ? extends K> function) {
			return null;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("(List<T>) listUnion");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("(List<T>) listUnion");
	}
}
