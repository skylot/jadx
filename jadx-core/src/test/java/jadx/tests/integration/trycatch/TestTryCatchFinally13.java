package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.inputs.InputPlugin;
import jadx.tests.api.extensions.inputs.TestWithInputPlugins;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally13 extends IntegrationTest {

	public static class TestCls {
		public void test(int i) {
			try {
				doSomething1();
				if (i == -12) {
					return;
				}
				if (i > 10) {
					doSomething2();
				} else if (i == -1) {
					doSomething3();
				}
			} catch (Exception ex) {
				logError();
			} finally {
				doSomething4();
			}
		}

		private void logError() {
		}

		private void doSomething1() {
		}

		private void doSomething2() {
		}

		private void doSomething3() {
		}

		private void doSomething4() {
		}
	}

	@TestWithInputPlugins({ InputPlugin.DEX, InputPlugin.JAVA })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} finally {");
	}
}
