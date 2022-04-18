package jadx.tests.integration.types;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Issue 1238
 */
public class TestTypeResolver20 extends SmaliTest {

	public static class TestCls {
		public interface Sequence<T> {
			Iterator<T> iterator();
		}

		public static <T extends Comparable<? super T>> T max(Sequence<? extends T> seq) {
			Iterator<? extends T> it = seq.iterator();
			if (!it.hasNext()) {
				return null;
			}
			T t = it.next();
			while (it.hasNext()) {
				T next = it.next();
				if (t.compareTo(next) < 0) {
					t = next;
				}
			}
			return t;
		}

		private static class ArraySeq<T> implements Sequence<T> {
			private final List<T> list;

			@SafeVarargs
			public ArraySeq(T... arr) {
				this.list = Arrays.asList(arr);
			}

			@Override
			public Iterator<T> iterator() {
				return list.iterator();
			}
		}

		public void check() {
			assertThat(max(new ArraySeq<>(2, 5, 3, 4))).isEqualTo(5);
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.JAVA8 })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("next = next;")
				.containsOne("T next = it.next();");
	}

	@Test
	public void testSmali() {
		assertThat(getClassNodeFromSmaliFiles())
				.code()
				.containsOne("T next = it.next();")
				.containsOne("T next2 = it.next();");
	}
}
