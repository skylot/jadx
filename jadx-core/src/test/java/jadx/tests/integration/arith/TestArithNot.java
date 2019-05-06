package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

public class TestArithNot extends SmaliTest {
	// @formatter:off
	/*
	  Smali Code equivalent:
		public static class TestCls {
			public int test1(int a) {
				return ~a;
			}

			public long test2(long b) {
				return ~b;
			}
		}
	*/
	// @formatter:on

	@Test
	public void test() {
		ClassNode cls = getClassNodeFromSmaliWithPath("arith", "TestArithNot");
		String code = cls.getCode().toString();

		assertThat(code, containsString("return ~a;"));
		assertThat(code, containsString("return ~b;"));
		assertThat(code, not(containsString("^")));
	}
}
