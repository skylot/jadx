package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestCmpOp2 extends IntegrationTest {

	public static class TestCls {
		public boolean testGT(float a, float b) {
			return a > b;
		}

		public boolean testLT(float c, double d) {
			return c < d;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return a > b;")
				.contains("return ((double) c) < d;");
	}
}
