package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestCmpOp extends IntegrationTest {

	public static class TestCls {
		public boolean testGT(float a) {
			return a > 3.0f;
		}

		public boolean testLT(float b) {
			return b < 2.0f;
		}

		public boolean testEQ(float c) {
			return c == 1.0f;
		}

		public boolean testNE(float d) {
			return d != 0.0f;
		}

		public boolean testGE(float e) {
			return e >= -1.0f;
		}

		public boolean testLE(float f) {
			return f <= -2.0f;
		}

		public boolean testGT2(float g) {
			return 4.0f > g;
		}

		public boolean testLT2(long h) {
			return 5 < h;
		}

		public boolean testGE2(double i) {
			return 6.5d < i;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return a > 3.0f;")
				.contains("return b < 2.0f;")
				.contains("return c == 1.0f;")
				.contains("return d != 0.0f;")
				.contains("return e >= -1.0f;")
				.contains("return f <= -2.0f;")
				.contains("return 4.0f > g;")
				.contains("return 5 < h;").contains("return 6.5d < i;");
	}
}
