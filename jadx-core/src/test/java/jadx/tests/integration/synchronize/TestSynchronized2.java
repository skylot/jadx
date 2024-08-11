package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestSynchronized2 extends IntegrationTest {

	public static class TestCls {
		@SuppressWarnings("unused")
		private static synchronized boolean test(Object obj) {
			return obj.toString() != null;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("private static synchronized boolean test(Object obj) {")
				.contains("obj.toString() != null;");
	}

	@Test
	@NotYetImplemented
	public void test2() {
		useDexInput(); // java bytecode don't add exception handlers

		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("return obj.toString() != null;")
				.doesNotContain("synchronized (");
	}
}
