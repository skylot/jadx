package jadx.tests.integration.arrays;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestArrayInitField extends IntegrationTest {

	public static class TestCls {
		static byte[] a = new byte[] { 10, 20, 30 };
		byte[] b = new byte[] { 40, 50, 60 };
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("static byte[] a = {10, 20, 30};")
				.containsOne("byte[] b = {40, 50, 60};");
	}
}
