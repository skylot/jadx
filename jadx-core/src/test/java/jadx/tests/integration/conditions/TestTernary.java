package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTernary extends IntegrationTest {

	public static class TestCls {
		public boolean test1(int a) {
			return a != 2;
		}

		public void test2(int a) {
			checkTrue(a == 3);
		}

		public int test3(int a) {
			return a > 0 ? a : (a + 2) * 3;
		}

		private static void checkTrue(boolean v) {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("else")
				.contains("return a != 2;")
				.contains("checkTrue(a == 3)")
				.contains("return a > 0 ? a : (a + 2) * 3;");
	}
}
