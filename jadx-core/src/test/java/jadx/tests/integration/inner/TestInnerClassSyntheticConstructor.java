package jadx.tests.integration.inner;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

public class TestInnerClassSyntheticConstructor extends IntegrationTest {

	private class TestCls {
		private int mth() {
			return 1;
		}
	}

	public int call() {
		return new TestCls().mth();
	}

	@Test
	public void test() {
		getClassNode(TestInnerClassSyntheticConstructor.class);
		// must compile, no usage of removed synthetic empty class
	}
}
