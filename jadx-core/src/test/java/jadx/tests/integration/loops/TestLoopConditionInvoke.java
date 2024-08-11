package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestLoopConditionInvoke extends IntegrationTest {

	public static class TestCls {
		private static final char STOP_CHAR = 0;
		private int pos;

		public boolean test(char lastChar) {
			int startPos = pos;
			char ch;
			while ((ch = next()) != STOP_CHAR) {
				if (ch == lastChar) {
					return true;
				}
			}
			pos = startPos;
			return false;
		}

		private char next() {
			return 0;
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("do {")
				.containsOne("if (ch == 0) {")
				.containsOne("this.pos = startPos;")
				.containsOne("return false;")
				.containsOne("} while (ch != lastChar);")
				.containsOne("return true;");
	}
}
