package jadx.tests.integration.invoke;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInvokeInCatch extends IntegrationTest {

	public static class TestCls {
		private static final String TAG = "TAG";

		public void test(int[] a, int b) {
			try {
				exc();
			} catch (IOException e) {
				if (b == 1) {
					log(TAG, "Error: {}", e.getMessage());
				}
			}
		}

		private static void log(String tag, String str, String... args) {
		}

		private void exc() throws IOException {
			throw new IOException();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("exc();")
				.doesNotContain("return;")
				.containsOne("} catch (IOException e) {")
				.containsOne("if (b == 1) {")
				.containsOne("log(TAG, \"Error: {}\", e.getMessage());");
	}
}
