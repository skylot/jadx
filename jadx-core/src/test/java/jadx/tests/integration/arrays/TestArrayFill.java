package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrayFill extends IntegrationTest {

	public static class TestCls {

		public String[] method() {
			return new String[] { "1", "2", "3" };
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return new String[]{\"1\", \"2\", \"3\"};");
	}
}
