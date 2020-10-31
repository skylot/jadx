package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 1002
 * Insertion of additional cast (at use place) needed for successful type inference
 */
public class TestTypeResolver16 extends SmaliTest {
	// @formatter:off
	/*
		public final <T, K> List<T> test(List<? extends T> list, Set<? extends T> set, Function<? super T, ? extends K> function) {
			checkParameterIsNotNull(function, "distinctBy");
			if (set != null) {
				List<? extends T> union = list != null ? union(list, set, function) : null;
				if (union != null) {
					list = union;
				}
			}
			return list != null ? (List<T>) list : emptyList();
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		assertThat(getClassNodeFromSmali())
				.code()
				.containsOne("(List<T>) list");
	}
}
