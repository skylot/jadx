package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestLoopCondition2 extends IntegrationTest {

	public static class TestCls {

		public int test(boolean a) {
			int i = 0;
			while (a && i < 10) {
				i++;
			}
			return i;
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("int i = 0;"));
		assertThat(code, containsOne("while (a && i < 10) {"));
		assertThat(code, containsOne("i++;"));
		assertThat(code, containsOne("return i;"));
	}
}
