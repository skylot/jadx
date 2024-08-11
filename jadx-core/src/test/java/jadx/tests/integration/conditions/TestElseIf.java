package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

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
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("} else if (str.equals(\"b\")) {")
				.containsOne("} else {")
				.containsOne("int r;")
				.containsOne("r = 1;")
				.containsOne("r = -1;")
				.doesNotContain(" ? ")
				.doesNotContain(" : "); // no ternary operator
	}
}
