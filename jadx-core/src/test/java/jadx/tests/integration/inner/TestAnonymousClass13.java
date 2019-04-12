package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestAnonymousClass13 extends IntegrationTest {

	public static class TestCls {

		public void test() {
			new TestCls() {
			};
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		getClassNode(TestCls.class);
	}
}
