package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestInline extends IntegrationTest {

	public static class TestCls {
		public static void main(String[] args) throws Exception {
			System.out.println("Test: " + new TestCls().testRun());
		}

		private boolean testRun() {
			return false;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("System.out.println(\"Test: \" + new TestInline$TestCls().testRun());");
	}
}
