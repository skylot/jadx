package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestCastInArith extends IntegrationTest {

	public static class TestCls {
		public float floatDiv(int a, int b) {
			return (float) a / (float) b;
		}

		public double doubleDiv(long a, long b) {
			return (double) a / (double) b;
		}

		public void check() {
			assertThat(floatDiv(1, 2)).isEqualTo(0.5f);
			assertThat(doubleDiv(1, 2)).isEqualTo(0.5);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOneOf("return (float) a / (float) b;", "return ((float) a) / ((float) b);")
				.containsOneOf("return (double) a / (double) b;", "return ((double) a) / ((double) b);");
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOneOf("(float) i / (float) i2", "((float) i) / ((float) i2)")
				.containsOneOf("(double) j / (double) j2", "((double) j) / ((double) j2)");
	}
}
