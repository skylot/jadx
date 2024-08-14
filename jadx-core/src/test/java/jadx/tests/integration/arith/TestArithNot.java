package jadx.tests.integration.arith;

import org.junit.jupiter.api.Test;

import jadx.tests.api.SmaliTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNodeFromSmaliWithPath("arith", "TestArithNot"))
				.code()
				.contains("return ~a;")
				.contains("return ~b;")
				.doesNotContain("^");
	}
}
