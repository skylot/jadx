package jadx.tests.integration.variables;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.inputs.InputPlugin;
import jadx.tests.api.extensions.inputs.TestWithInputPlugins;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVariablesInInlinedAssign extends IntegrationTest {

	public static class TestCls {
		public final int test(final char[] s) {
			int i;
			for (i = 0; i < s.length; i++) {
				final char c = s[i];
				if (c != 'a' && c != 'b') {
					break;
				}
			}
			return i;
		}
	}

	@TestWithInputPlugins({ InputPlugin.DEX, InputPlugin.JAVA })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("char c");
	}
}
