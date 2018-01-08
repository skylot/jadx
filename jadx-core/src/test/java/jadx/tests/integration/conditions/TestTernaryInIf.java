package jadx.tests.integration.conditions;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

public class TestTernaryInIf extends IntegrationTest {

	public static class TestCls {
		public boolean test1(boolean a, boolean b, boolean c) {
			return a ? b : c;
		}

		public int test2(boolean a, boolean b, boolean c) {
			return (a ? b : c) ? 1 : 2;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("return a ? b : c;"));
		assertThat(code, containsOne("return (a ? b : c) ? 1 : 2;"));
		assertThat(code, not(containsString("if")));
		assertThat(code, not(containsString("else")));
	}
}
