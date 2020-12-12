package jadx.tests.integration.invoke;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVarArg2 extends IntegrationTest {

	@SuppressWarnings("ConstantConditions")
	public static class TestCls {
		protected static boolean b1;
		protected static final boolean IS_VALID = b1 && isValid("test");

		private static boolean isValid(String... string) {
			return false;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("isValid(\"test\")"); // TODO: .containsOne("b1 && isValid(\"test\")");
	}
}
