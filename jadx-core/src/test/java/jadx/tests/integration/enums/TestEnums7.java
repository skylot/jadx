package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestEnums7 extends IntegrationTest {

	public static class TestCls {
		public enum Numbers {
			ZERO,
			ONE;

			private final int n;

			Numbers() {
				this.n = this.name().equals("ZERO") ? 0 : 1;
			}

			public int getN() {
				return n;
			}
		}

		public void check() {
			assertThat(Numbers.ZERO.getN()).isEqualTo(0);
			assertThat(Numbers.ONE.getN()).isEqualTo(1);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ZERO,")
				.containsOne("ONE;")
				.containsOne("Numbers() {");
	}
}
