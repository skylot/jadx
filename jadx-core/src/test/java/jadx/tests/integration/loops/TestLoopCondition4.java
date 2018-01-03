package jadx.tests.integration.loops;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.junit.Assert.assertThat;

public class TestLoopCondition4 extends IntegrationTest {

	public static class TestCls {
		public static void test() {
			int n = -1;
			while (n < 0) {
				n += 12;
			}
			while (n > 11) {
				n -= 12;
			}
			System.out.println(n);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("int n = -1;"));
		assertThat(code, containsOne("while (n < 0) {"));
		assertThat(code, containsOne("n += 12;"));
		assertThat(code, containsOne("while (n > 11) {"));
		assertThat(code, containsOne("n -= 12;"));
		assertThat(code, containsOne("System.out.println(n);"));
	}
}
