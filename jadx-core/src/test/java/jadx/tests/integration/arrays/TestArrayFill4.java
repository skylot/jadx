package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayFill4 extends IntegrationTest {

	public static class TestCls {

		// replaced constant break filled array creation
		private static final int ARRAY_SIZE = 4;

		public long[] test() {
			return new long[] { 0, 1, Long.MAX_VALUE, Long.MIN_VALUE + 1 };
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("new long[ARRAY_SIZE];")
				.containsOne("return new long[]{0, 1, ");
	}
}
