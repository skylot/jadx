package jadx.tests.integration.java8;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestLambdaInstance2 extends IntegrationTest {

	public static class TestCls {
		private String field;

		public Runnable test(String str, int i) {
			return () -> call(str, i);
		}

		public void call(String str, int i) {
			field = str + '=' + i;
		}

		public void check() throws Exception {
			field = "";
			test("num", 7).run();
			assertThat(field).isEqualTo("num=7");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("lambda$")
				.containsOne("call(str, i)");
	}
}
