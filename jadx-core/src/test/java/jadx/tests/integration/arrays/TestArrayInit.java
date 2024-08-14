package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestArrayInit extends IntegrationTest {

	public static class TestCls {

		byte[] bytes;

		@SuppressWarnings("unused")
		public void test() {
			byte[] arr = new byte[] { 10, 20, 30 };
		}

		public void test2() {
			bytes = new byte[] { 10, 20, 30 };
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("= {10, 20, 30};")
				.contains("this.bytes = new byte[]{10, 20, 30};");
	}
}
