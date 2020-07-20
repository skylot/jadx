package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSpecialValues2 extends IntegrationTest {

	public static class TestCls {
		private static int compareUnsigned(final int x, final int y) {
			return Integer.compare(x + Integer.MIN_VALUE, y + Integer.MIN_VALUE);
		}
	}

	@NotYetImplemented("Constant value replace")
	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "Integer.MIN_VALUE");
	}
}
