package jadx.tests.integration.enums;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestEnums3 extends IntegrationTest {

	public static class TestCls {

		private static int three = 3;

		public enum Numbers {
			ONE(1), TWO(2), THREE(three), FOUR(three + 1);

			private final int num;

			Numbers(int n) {
				this.num = n;
			}

			public int getNum() {
				return num;
			}
		}

		public void check() {
			assertThat(Numbers.ONE.getNum()).isEqualTo(1);
			assertThat(Numbers.THREE.getNum()).isEqualTo(3);
			assertThat(Numbers.FOUR.getNum()).isEqualTo(4);
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("ONE(1)")
				.containsOne("Numbers(int n) {");
		// assertThat(code, containsOne("THREE(three)"));
		// assertThat(code, containsOne("assertTrue(Numbers.ONE.getNum() == 1);"));
	}
}
