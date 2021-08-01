package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayFillConstReplace extends IntegrationTest {

	public static class TestCls {
		public static final int CONST_INT = 0xffff;

		public int[] test() {
			return new int[] { 127, 129, CONST_INT };
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(" int CONST_INT = 65535;")
				.containsOne("return new int[]{127, 129, CONST_INT};");
	}
}
