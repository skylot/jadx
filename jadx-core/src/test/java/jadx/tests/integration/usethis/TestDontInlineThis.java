package jadx.tests.integration.usethis;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestDontInlineThis extends IntegrationTest {

	public static class TestCls {
		public int field = new Random().nextInt();

		public TestCls test() {
			TestCls res;
			if (field == 7) {
				res = this;
				System.out.println();
			} else {
				res = new TestCls();
			}
			res.method();
			return res;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("TestDontInlineThis$TestCls res")
				.containsOne("res = this;")
				.containsOne("res = new TestDontInlineThis$TestCls();");
	}
}
