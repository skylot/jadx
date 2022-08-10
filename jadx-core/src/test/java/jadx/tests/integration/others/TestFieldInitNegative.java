package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * Negative case for field initialization move (#1599).
 * Can't reorder with other instance methods.
 */
public class TestFieldInitNegative extends IntegrationTest {

	public static class TestCls {
		StringBuilder sb;
		int field;

		public TestCls() {
			initBuilder(new StringBuilder("sb"));
			this.field = initField();
			this.sb.append(this.field);
		}

		private void initBuilder(StringBuilder sb) {
			this.sb = sb;
		}

		private int initField() {
			return sb.length();
		}

		public String getStr() {
			return sb.toString();
		}

		public void check() {
			assertThat(new TestCls().getStr()).isEqualTo("sb2"); // no NPE
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("int field = initField();")
				.containsOne("this.field = initField();");
	}
}
