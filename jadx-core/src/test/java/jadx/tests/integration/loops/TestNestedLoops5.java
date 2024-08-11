package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

public class TestNestedLoops5 extends IntegrationTest {

	public static class TestCls {

		public int testFor() {
			int tmp = 1;
			for (int i = 5; i > -1; i--) {
				if (i > tmp) {
					for (int j = 1; j < 5; j++) {
						if (tmp > j * 100) {
							return 0;
						}
					}
				}
				tmp++;
			}
			return tmp;
		}

		public void check() {
			assertThat(testFor()).isEqualTo(7);
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("continue;");
	}
}
