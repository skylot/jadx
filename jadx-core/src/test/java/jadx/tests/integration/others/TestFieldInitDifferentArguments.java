package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFieldInitDifferentArguments extends IntegrationTest {

	public static class TestCls {
		public final String value;

		public TestCls() {
			this.value = String.valueOf(Math.abs(-1));
		}

		public TestCls(int ignored) {
			this.value = String.valueOf(Math.abs(-2));
		}

		public void check() {
			assertThat(new TestCls().value).isEqualTo("1");
			assertThat(new TestCls(0).value).isEqualTo("2");
		}
	}

	@Test
	public void test() {
		getArgs().setDebugInfo(false);
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("final String value;")
				.containsOne("this.value = String.valueOf(Math.abs(-1));")
				.containsOne("this.value = String.valueOf(Math.abs(-2));");
	}
}
