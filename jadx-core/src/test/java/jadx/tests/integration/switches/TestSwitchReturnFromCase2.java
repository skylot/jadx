package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.utils.assertj.JadxAssertions;

import static org.assertj.core.api.Assertions.assertThat;

public class TestSwitchReturnFromCase2 extends IntegrationTest {

	public static class TestCls {
		public boolean test(int a) {
			switch (a % 4) {
				case 2:
				case 3:
					if (a == 2) {
						return true;
					}
					return true;
			}
			return false;
		}

		public void check() {
			assertThat(test(2)).isTrue();
			assertThat(test(3)).isTrue();
			assertThat(test(15)).isTrue();
			assertThat(test(1)).isFalse();
		}
	}

	@Test
	public void test() {
		JadxAssertions.assertThat(getClassNode(TestCls.class))
				.code()
				.contains("switch (a % 4) {");
	}
}
