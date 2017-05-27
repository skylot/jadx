package jadx.tests.integration.inline;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

public class TestInlineInLoop extends IntegrationTest {

	public static class TestCls {
		public static void main(String[] args) throws Exception {
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

		assertThat(code, containsOne("int c"));
		assertThat(code, containsOne("c = b + 1"));
		assertThat(code, countString(2, "c = b;"));
		assertThat(code, containsOne("b++;"));
		assertThat(code, containsOne("b = c"));
		assertThat(code, containsOne("a++;"));
	}
}
