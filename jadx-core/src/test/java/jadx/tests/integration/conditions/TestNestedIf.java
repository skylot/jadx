package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedIf extends IntegrationTest {

	public static class TestCls {
		private boolean a0 = false;
		private int a1 = 1;
		private int a2 = 2;
		private int a3 = 1;
		private int a4 = 2;

		public boolean test1() {
			if (a0) {
				if (a1 == 0 || a2 == 0) {
					return false;
				}
			} else if (a3 == 0 || a4 == 0) {
				return false;
			}
			test1();
			return true;
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (this.a0) {")
				.containsOne("if (this.a1 == 0 || this.a2 == 0) {")
				.containsOne("} else if (this.a3 == 0 || this.a4 == 0) {")
				.countString(2, "return false;")
				.containsOne("test1();")
				.containsOne("return true;");
	}
}
