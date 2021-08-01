package jadx.tests.integration.variables;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestVariables5 extends IntegrationTest {

	public static class TestCls {
		public String f = "str//ing";
		private boolean enabled;

		private void testIfInLoop() {
			int i = 0;
			for (int i2 = 0; i2 < f.length(); i2++) {
				char ch = f.charAt(i2);
				if (ch == '/') {
					i++;
					if (i == 2) {
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
		assertThat(cls)
				.code()
				.doesNotContain("int i2++;")
				.containsOne("int i = 0;")
				.containsOneOf("i++;", "&& (i = i + 1) == 2");
	}
}
