package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

public class TestAnonymousClass13 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			new TestCls() {
			};
		}
	}

	@Test
	public void test() {
		getClassNode(TestCls.class);
	}
}
