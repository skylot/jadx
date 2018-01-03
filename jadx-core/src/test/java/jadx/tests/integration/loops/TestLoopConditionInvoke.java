package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopConditionInvoke extends IntegrationTest {

	public static class TestCls {
		private static final char STOP_CHAR = 0;
		private int pos;

		private boolean test(char lastChar) {
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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("do {"));
		assertThat(code, containsOne("if (ch == '\\u0000') {"));
		assertThat(code, containsOne("this.pos = startPos;"));
		assertThat(code, containsOne("return false;"));
		assertThat(code, containsOne("} while (ch != lastChar);"));
		assertThat(code, containsOne("return true;"));
	}
}
