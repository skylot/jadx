package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopCondition extends IntegrationTest {

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

		assertThat(code, containsOne("i < this.f.length()"));
		assertThat(code, containsOne("list.set(i, \"ABC\")"));
		assertThat(code, containsOne("list.set(i, \"DEF\")"));

		assertThat(code, containsOne("if (j == 2) {"));
		assertThat(code, containsOne("setEnabled(true);"));
		assertThat(code, containsOne("setEnabled(false);"));
	}
}
