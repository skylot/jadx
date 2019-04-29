package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestTernary extends IntegrationTest {

	public static class TestCls {
		public boolean test1(int a) {
			return a != 2;
		}

		public void test2(int a) {
			checkTrue(a == 3);
		}

		public int test3(int a) {
			return a > 0 ? a : (a + 2) * 3;
		}

		private static void checkTrue(boolean v) {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, not(containsString("else")));
		assertThat(code, containsString("return a != 2;"));
		assertThat(code, containsString("checkTrue(a == 3)"));
		assertThat(code, containsString("return a > 0 ? a : (a + 2) * 3;"));
	}
}
