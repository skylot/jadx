package jadx.tests.integration.generics;

import java.util.List;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenerics extends IntegrationTest {

	public static class TestCls {
		class A {
		}

		public static void mthWildcard(List<?> list) {
		}

		public static void mthExtends(List<? extends A> list) {
		}

		public static void mthSuper(List<? super A> list) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("mthWildcard(List<?> list)")
				.contains("mthExtends(List<? extends A> list)")
				.contains("mthSuper(List<? super A> list)");
	}
}
