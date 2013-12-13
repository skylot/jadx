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

		private void testMoreComplexIfInLoop(java.util.ArrayList<String> list) throws Exception {
			for (int i = 0; i != 16 && i < 255; i++) {
				list.set(i, "ABC");
				if (i == 128) {
					return;
				}
				list.set(i, "DEF");
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsString("i < this.f.length()"));
		assertThat(code, containsString("while (a && i < 10) {"));
		assertThat(code, containsString("list.set(i, \"ABC\")"));
		assertThat(code, containsString("list.set(i, \"DEF\")"));
	}
}
