package jadx.tests.integration.inline;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.MatcherAssert.assertThat;

public class TestInlineInLoop extends IntegrationTest {

	public static class TestCls {
		public static void main(String[] args) {
			int a = 0;
			int b = 4;
			int c = 0;
			while (a < 12) {
				if (b + a < 9 && b < 8) {
					if (b >= 2 && a > -1 && b < 6) {
						System.out.println("OK");
						c = b + 1;
					}
					c = b;
				}
				c = b;
				b++;
				b = c;
				a++;
			}
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		// TODO: remove unused variables from test
		assertThat(code, containsOne("int c = b + 1"));
		assertThat(code, containsOne("int c2 = b;"));
		assertThat(code, containsOne("int c3 = b;"));
		assertThat(code, containsOne("int b2 = b + 1;"));
		assertThat(code, containsOne("b = c3"));
		assertThat(code, containsOne("a++;"));
	}
}
