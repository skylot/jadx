package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestVarArg extends IntegrationTest {

	public static class TestCls {

		public void test1(int... a) {
		}

		public void test2(int i, Object... a) {
		}

		public void test3(int[] a) {
		}

		public void call() {
			test1(1, 2);
			test2(3, "1", 7);
			test3(new int[] { 5, 8 });
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("void test1(int... a) {"));
		assertThat(code, containsString("void test2(int i, Object... a) {"));

		assertThat(code, containsString("test1(1, 2);"));
		assertThat(code, containsString("test2(3, \"1\", 7);"));

		// negative case
		assertThat(code, containsString("void test3(int[] a) {"));
		assertThat(code, containsString("test3(new int[]{5, 8});"));
	}
}
