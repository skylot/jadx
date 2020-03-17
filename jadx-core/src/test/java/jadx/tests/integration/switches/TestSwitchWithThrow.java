package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchWithThrow extends IntegrationTest {

	public static class TestCls {
		public int test(int i) {
			if (i != 0) {
				switch (i % 4) {
					case 1:
						throw new IllegalStateException("1");
					case 2:
						throw new IllegalStateException("2");
					default:
						throw new IllegalStateException("Other");
				}
			} else {
				System.out.println("0");
				return -1;
			}
		}

		public void check() {
			assertThat(test(0)).isEqualTo(-1);
			// TODO: implement 'invoke-custom' support
			// assertThat(catchThrowable(() -> test(1)))
			// .isInstanceOf(IllegalStateException.class).hasMessageContaining("1");
			// assertThat(catchThrowable(() -> test(3)))
			// .isInstanceOf(IllegalStateException.class).hasMessageContaining("Other");
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.contains("throw new IllegalStateException(\"1\");");
	}
}
