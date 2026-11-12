package jadx.tests.integration.trycatch;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchThrowInside extends IntegrationTest {

	public static class TestCls {
		public String test(int i) {
			StringBuilder sb = new StringBuilder();
			try {
				if (i == 1) {
					throw new IllegalStateException("bad");
				}
				sb.append("ok");
			} catch (IllegalStateException e) {
				sb.append(e.getMessage());
			}
			sb.append('.');
			return sb.toString();
		}

		public void check() {
			assertThat(test(1)).isEqualTo("bad.");
			assertThat(test(2)).isEqualTo("ok.");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("sb.append('.');");
	}

	@Test
	public void testJavaInput() {
		useJavaInput();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("sb.append('.');");
	}
}
