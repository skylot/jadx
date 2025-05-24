package jadx.tests.integration.synchronize;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSynchronized2 extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		private static synchronized boolean test(Object obj) {
			return obj.toString() != null;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("private static synchronized boolean test(Object obj) {")
				.doesNotContain("synchronized (")
				.contains("obj.toString() != null;");
	}
}
