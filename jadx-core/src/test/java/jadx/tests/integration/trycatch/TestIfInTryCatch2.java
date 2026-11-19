package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestIfInTryCatch2 extends IntegrationTest {

	public static class TestCls {
		private static void check(int i) {
			if (i == 1) {
				throw new IllegalStateException("bad");
			}
		}

		public String test(int i) {
			StringBuilder sb = new StringBuilder();
			try {
				if (i > 0) {
					check(i);
				}
				sb.append("ok");
			} catch (IllegalStateException e) {
				sb.append(e.getMessage());
			}
			sb.append('.');
			return sb.toString();
		}

		public void check() {
			org.assertj.core.api.Assertions.assertThat(test(0)).isEqualTo("ok.");
			org.assertj.core.api.Assertions.assertThat(test(1)).isEqualTo("bad.");
			org.assertj.core.api.Assertions.assertThat(test(2)).isEqualTo("ok.");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("try {")
				.containsOne("} catch (IllegalStateException e) {");
	}
}
