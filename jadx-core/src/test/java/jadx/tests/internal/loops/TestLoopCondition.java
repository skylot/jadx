package jadx.tests.internal.loops;

import jadx.api.InternalJadxTest;
import jadx.core.dex.nodes.ClassNode;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

public class TestLoopCondition extends InternalJadxTest {

	public static class TestCls {
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
		System.out.println(code);

		assertThat(code, containsString("i < this.f.length()"));
		assertThat(code, containsString("list.set(i, \"ABC\")"));
		assertThat(code, containsString("list.set(i, \"DEF\")"));
	}
}
