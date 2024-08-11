package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestTryCatch extends IntegrationTest {

	public static class TestCls {
		public void f() {
			try {
				Thread.sleep(50L);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("try {")
				.contains("Thread.sleep(50L);")
				.contains("} catch (InterruptedException e) {")
				.doesNotContain("return");
	}
}
