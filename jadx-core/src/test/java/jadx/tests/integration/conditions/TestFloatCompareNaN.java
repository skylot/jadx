package jadx.tests.integration.conditions;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestFloatCompareNaN extends IntegrationTest {

	@SuppressWarnings({ "SimplifiableConditionalExpression", "ExpressionComparedToItself" })
	public static class TestCls {
		public int notLess(double a, double b) {
			if (!(a < b)) {
				return 1;
			}
			return 2;
		}

		public int notGreaterOrEqual(float a, float b) {
			if (!(a >= b)) {
				return 1;
			}
			return 2;
		}

		public boolean notGreater(double a, double b) {
			return !(a > b);
		}

		public String notLessOrEqual(float a, float b) {
			return !(a <= b) ? "x" : "y";
		}

		public void check() {
			assertThat(notLess(Double.NaN, 1.0)).isEqualTo(1);
			assertThat(notLess(1.0, 2.0)).isEqualTo(2);
			assertThat(notLess(2.0, 1.0)).isEqualTo(1);

			assertThat(notGreaterOrEqual(Float.NaN, 1.0f)).isEqualTo(1);
			assertThat(notGreaterOrEqual(1.0f, 2.0f)).isEqualTo(1);
			assertThat(notGreaterOrEqual(2.0f, 1.0f)).isEqualTo(2);

			assertThat(notGreater(Double.NaN, 1.0)).isTrue();
			assertThat(notGreater(1.0, 2.0)).isTrue();
			assertThat(notGreater(2.0, 1.0)).isFalse();

			assertThat(notLessOrEqual(1.0f, Float.NaN)).isEqualTo("x");
			assertThat(notLessOrEqual(2.0f, 1.0f)).isEqualTo("x");
			assertThat(notLessOrEqual(1.0f, 2.0f)).isEqualTo("y");
		}
	}

	@TestWithProfiles({ TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (!(a < b)) {")
				.containsOne("if (!(a >= b)) {")
				.containsOne("return !(a > b);")
				.containsOne("return !(a <= b) ? \"x\" : \"y\";");
	}
}
