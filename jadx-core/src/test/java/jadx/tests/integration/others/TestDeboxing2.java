package jadx.tests.integration.others;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDeboxing2 extends IntegrationTest {

	public static class TestCls {
		public long test(Long l) {
			if (l == null) {
				l = 0L;
			}
			return l;
		}

		public void check() {
			assertThat(test(null)).isEqualTo(0L);
			assertThat(test(0L)).isEqualTo(0L);
			assertThat(test(7L)).isEqualTo(7L);
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("long test(Long l)")
				.containsOne("if (l == null) {")
				.containsOne("l = 0L;")
				.containsOne("test(null)")
				.containsOne("test(0L)")
				// checks for 'check' method
				.countString(2, "isEqualTo(0L)")
				.containsOne("test(7L)")
				.containsOne("isEqualTo(7L)");
	}
}
