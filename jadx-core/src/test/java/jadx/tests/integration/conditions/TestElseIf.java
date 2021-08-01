package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.containsOne;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

@SuppressWarnings("IfCanBeSwitch")
public class TestElseIf extends IntegrationTest {

	public static class TestCls {
		public int testIfElse(String str) {
			int r;
			if (str.equals("a")) {
				r = 1;
			} else if (str.equals("b")) {
				r = 2;
			} else if (str.equals("3")) {
				r = 3;
			} else if (str.equals("$")) {
				r = 4;
			} else {
				r = -1;
				System.out.println();
			}
			r = r * 10;
			return Math.abs(r);
		}
	}

	@Test
	public void test() {
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

		assertThat(code, containsOne("} else if (str.equals(\"b\")) {"));
		assertThat(code, containsOne("} else {"));
		assertThat(code, containsOne("int r;"));
		assertThat(code, containsOne("r = 1;"));
		assertThat(code, containsOne("r = -1;"));
		// no ternary operator
		assertThat(code, not(containsString(" ? ")));
		assertThat(code, not(containsString(" : ")));
	}
}
