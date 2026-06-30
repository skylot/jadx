package jadx.tests.integration.types;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestLongCast extends IntegrationTest {

	public static class TestCls {

		public long test(char c) {
			return (long) c << 32;
		}

		public int test2(long l) {
			return (int) l >> 2;
		}

		public void check() {
			assertThat(test((char) 22)).isEqualTo(94489280512L);
			assertThat(test2((1L << 32) + 8)).isEqualTo(2);
		}
	}

	@Test
	public void test() {
		noDebugInfo();
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOneOf(
						"return (long) c << 32;",
						"return ((long) c) << 32;");
	}
}
