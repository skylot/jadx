package jadx.tests.integration.variables;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestVariables5 extends IntegrationTest {

	public static class TestCls {
		public String f = "str//ing";
		private boolean enabled;

		private void testIfInLoop() {
			int j = 0;
			for (int i = 0; i < f.length(); i++) {
				char ch = f.charAt(i);
				if (ch == '/') {
					j++;
					if (j == 2) {
						setEnabled(true);
						return;
					}
				}
			}
			setEnabled(false);
		}

		private void setEnabled(boolean b) {
			this.enabled = b;
		}

		public void check() {
			setEnabled(false);
			testIfInLoop();
			assertTrue(enabled);
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("int i2++;")));
		assertThat(code, containsOne("int i = 0;"));
		assertThat(code, containsOne("i++;"));
	}
}
