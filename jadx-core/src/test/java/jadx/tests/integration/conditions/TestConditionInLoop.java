package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class TestConditionInLoop extends IntegrationTest {

	public static class TestCls {
		private static int test(int a, int b) {
			int c = a + b;
			for (int i = a; i < b; i++) {
				if (i == 7) {
					c += 2;
				} else {
					c *= 2;
				}
			}
			c--;
			return c;
		}

		public void check() {
			assertThat(test(5, 9), is(115));
			assertThat(test(8, 23), is(1015807));
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("for (int i = a; i < b; i++) {"));
		assertThat(code, containsOne("c += 2;"));
		assertThat(code, containsOne("c *= 2;"));
	}

	@Test
	public void testNoDebug() {
		noDebugInfo();
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("while"));
	}
}
