package jadx.tests.integration.invoke;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.inputs.InputPlugin;
import jadx.tests.api.extensions.inputs.TestWithInputPlugins;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSuperInvoke2 extends IntegrationTest {

	public static class TestCls {
		@Override
		public String toString() {
			return super.toString();
		}

		public void check() {
			assertThat(new TestCls().toString()).containsOne("@");
		}
	}

	@TestWithInputPlugins({ InputPlugin.DEX, InputPlugin.JAVA })
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return super.toString();");
	}
}
