package jadx.tests.integration.inner;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	public void test(boolean showEmptySyntheticClasses) {
		args.setShowEmptySyntheticClasses(showEmptySyntheticClasses);
		getClassNode(TestInnerClassSyntheticConstructor.class);
		// must compile, no usage of removed synthetic empty class
	}
}
