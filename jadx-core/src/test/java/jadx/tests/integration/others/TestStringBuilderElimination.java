package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestStringBuilderElimination extends IntegrationTest {

	public static class MyException extends Exception {
		private static final long serialVersionUID = 4245254480662372757L;

		public MyException(String str, Exception e) {
			super("msg:" + str, e);
		}

		public void method(int k) {
			System.out.println("k=" + k);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(MyException.class))
				.code()
				.contains("MyException(String str, Exception e) {")
				.contains("super(\"msg:\" + str, e);")
				.doesNotContain("new StringBuilder")
				.contains("System.out.println(\"k=\" + k);");
	}
}
