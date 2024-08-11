package jadx.tests.integration.generics;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestGenericsInArgs extends IntegrationTest {

	public static class TestCls {
		public static <T> void test(List<? super T> genericList, Set<T> set) {
			if (genericList == null) {
				throw new RuntimeException("list is null");
			}
			if (set == null) {
				throw new RuntimeException("set is null");
			}
			genericList.clear();
			use(genericList);
			set.clear();
		}

		private static void use(List<?> l) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public static <T> void test(List<? super T> genericList, Set<T> set) {")
				.contains("if (genericList == null) {");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("public static <T> void test(List<? super T> list, Set<T> set) {")
				.contains("if (list == null) {");
	}
}
