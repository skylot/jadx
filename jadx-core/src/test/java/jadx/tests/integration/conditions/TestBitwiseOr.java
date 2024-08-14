package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

public class TestBitwiseOr extends IntegrationTest {

	public static class TestCls {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a | b) != false) {
				test();
			}
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("if (this.a || this.b) {");
	}

	public static class TestCls2 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a | b) != true) {
				test();
			}
		}
	}

	@Test
	public void test2() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls2.class))
				.code()
				.containsOne("if (!this.a && !this.b) {");
	}

	public static class TestCls3 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a | b) == false) {
				test();
			}
		}
	}

	@Test
	public void test3() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls3.class))
				.code()
				.containsOne("if (!this.a && !this.b) {");
	}

	public static class TestCls4 {
		private boolean a;
		private boolean b;

		public void test() {
			if ((a | b) == true) {
				test();
			}
		}
	}

	@Test
	public void test4() {
		noDebugInfo();
		JadxAssertions.assertThat(getClassNode(TestCls4.class))
				.code()
				.containsOne("if (this.a || this.b) {");
	}
}
