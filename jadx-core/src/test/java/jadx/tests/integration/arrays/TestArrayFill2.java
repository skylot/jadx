package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrayFill2 extends IntegrationTest {

	public static class TestCls {

		public int[] test(int a) {
			return new int[] { 1, a + 1, 2 };
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return new int[]{1, a + 1, 2};");
	}

	public static class TestCls2 {

		public int[] test2(int a) {
			return new int[] { 1, a++, a * 2 };
		}
	}

	@Test
	@NotYetImplemented
	public void test2() {
		JadxAssertions.assertThat(getClassNode(TestCls2.class))
				.code()
				.contains("return new int[]{1, a++, a * 2};");
	}
}
