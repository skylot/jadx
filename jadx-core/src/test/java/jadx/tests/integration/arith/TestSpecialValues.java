package jadx.tests.integration.arith;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("Float.NaN, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, " +
				"Float.MIN_VALUE, Float.MAX_VALUE, Float.MIN_NORMAL"));

		assertThat(code, containsOne("Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, " +
				"Double.MIN_VALUE, Double.MAX_VALUE, Double.MIN_NORMAL"));

		assertThat(code, containsOne("Short.MIN_VALUE, Short.MAX_VALUE"));
		assertThat(code, containsOne("Byte.MIN_VALUE, Byte.MAX_VALUE"));
		assertThat(code, containsOne("Integer.MIN_VALUE, Integer.MAX_VALUE"));
		assertThat(code, containsOne("Long.MIN_VALUE, Long.MAX_VALUE"));
	}
}
