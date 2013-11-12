package jadx.tests.internal;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestLoopCondition extends InternalJadxTest {

	@SuppressWarnings("serial")
	public static class TestCls extends Exception {
		public String f;

		private void setEnabled(boolean r1z) {
		}

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

		public int testComplexIfInLoop(boolean a) {
			int i = 0;
			while (a && i < 10) {
				i++;
			}
			return i;
		}
	}

	@Test
	public void test() {
		setOutputCFG();

		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("i < f.length()"));
		assertThat(code, containsString("while (a && i < 10) {"));
	}
}
