package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestEnums6 extends IntegrationTest {

	public static class TestCls {
		public enum Numbers {
			ZERO,
			ONE(1);

			private final int n;

			Numbers() {
				this(0);
			}

			Numbers(int n) {
				this.n = n;
			}

			public int getN() {
				return n;
			}
		}

		public void check() {
			assertThat(TestCls.Numbers.ZERO.getN()).isEqualTo(0);
			assertThat(TestCls.Numbers.ONE.getN()).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ZERO,")
				.containsOne("Numbers() {")
				.containsOne("ONE(1);");
	}
}
