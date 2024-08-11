package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

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
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("void test1(int... a) {")
				.contains("void test2(int i, Object... a) {")
				.contains("test1(1, 2);")
				.contains("test2(3, \"1\", 7);")
				// negative case
				.contains("void test3(int[] a) {")
				.contains("test3(new int[]{5, 8});");
	}
}
