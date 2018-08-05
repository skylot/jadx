package jadx.tests.integration.generics;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

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

		private static void use(List l) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public static <T> void test(List<? super T> genericList, Set<T> set) {"));
		assertThat(code, containsString("if (genericList == null) {"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("public static <T> void test(List<? super T> list, Set<T> set) {"));
		assertThat(code, containsString("if (list == null) {"));
	}
}
