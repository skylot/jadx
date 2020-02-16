package jadx.tests.integration.types;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTypeResolver11 extends IntegrationTest {

	public static class TestCls {
		public Void test(Object... objects) {
			int val = (Integer) objects[0];
			String str = (String) objects[1];
			call(str, str, val, val);
			return null;
		}

		private void call(String a, String b, int... val) {
		}

		private boolean test2(String s1, String... args) {
			String str = Arrays.toString(args);
			return s1.length() + str.length() > 0;
		}

		public void check() {
			test(1, "str");
			assertThat(test2("1", "2", "34")).isTrue();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("(Integer) objects[0]")
				.containsOne("String str = (String) objects[1];");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("(Integer) objArr[0]")
				.containsOne("String str = (String) objArr[1];");
	}
}
