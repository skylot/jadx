package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSpecialValues extends IntegrationTest {

	public static class TestCls {

		public void test() {
			shorts(Short.MIN_VALUE, Short.MAX_VALUE);
			bytes(Byte.MIN_VALUE, Byte.MAX_VALUE);
			ints(Integer.MIN_VALUE, Integer.MAX_VALUE);
			longs(Long.MIN_VALUE, Long.MAX_VALUE);

			floats(Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY,
					Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL);

			doubles(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY,
					Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL);
		}

		private void shorts(short... v) {
		}

		private void bytes(byte... v) {
		}

		private void ints(int... v) {
		}

		private void longs(long... v) {
		}

		private void floats(float... v) {
		}

		private void doubles(double... v) {
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(
						"Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL")
				.containsOne("Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, "
						+ "Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL")
				.containsOne("Short.MIN_VALUE, Short.MAX_VALUE")
				.containsOne("Byte.MIN_VALUE, Byte.MAX_VALUE")
				.containsOne("Integer.MIN_VALUE, Integer.MAX_VALUE")
				.containsOne("Long.MIN_VALUE, Long.MAX_VALUE");
	}
}
