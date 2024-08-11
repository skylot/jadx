package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestNestedTryCatch extends IntegrationTest {

	public static class TestCls {
		public void test() {
			try {
				Thread.sleep(1L);
				try {
					Thread.sleep(2L);
				} catch (InterruptedException ignored) {
					System.out.println(2);
				}
			} catch (Exception ignored) {
				System.out.println(1);
			}
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("try {")
				.contains("Thread.sleep(1L);")
				.contains("Thread.sleep(2L);")
				.contains("} catch (InterruptedException e) {")
				.contains("} catch (Exception e2) {")
				.doesNotContain("return");
	}
}
