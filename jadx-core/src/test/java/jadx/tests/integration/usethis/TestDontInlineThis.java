package jadx.tests.integration.usethis;

import java.util.Random;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestDontInlineThis extends IntegrationTest {

	public static class TestCls {
		public int field = new Random().nextInt();

		public TestCls test() {
			TestCls res;
			if (field == 7) {
				res = this;
				System.out.println();
			} else {
				res = new TestCls();
			}
			res.method();
			return res;
		}

		private void method() {
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("TestDontInlineThis$TestCls res"));
		assertThat(code, containsOne("res = this;"));
		assertThat(code, containsOne("res = new TestDontInlineThis$TestCls();"));
	}
}
