package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestInnerAssign2 extends IntegrationTest {

	public static class TestCls {
		private String field;
		private String swapField;

		@SuppressWarnings("checkstyle:InnerAssignment")
		public boolean test(String str) {
			String sub;
			return call(str) || ((sub = this.field) != null && sub.isEmpty());
		}

		private boolean call(String str) {
			this.field = swapField;
			return str.isEmpty();
		}

		public boolean testWrap(String str, String fieldValue) {
			this.field = null;
			this.swapField = fieldValue;
			return test(str);
		}

		public void check() {
			assertThat(testWrap("", null)).isTrue();
			assertThat(testWrap("a", "")).isTrue();
			assertThat(testWrap("b", null)).isFalse();
			assertThat(testWrap("c", "d")).isFalse();
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("sub = this.field")
				.containsOne("return call(str) || ((sub = this.field) != null && sub.isEmpty());");
	}
}
