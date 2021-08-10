package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedLoops4 extends IntegrationTest {

	public static class TestCls {

		public int testFor() {
			int tmp = 1;
			for (int i = 10; i > -1; i--) {
				if (i > tmp) {
					for (int j = 0; j < 54; j += 4) {
						if (i < j) {
							for (int k = j; k < j + 4; k++) {
								if (tmp > k) {
									return 0;
								}
							}
							break;
						}
					}
				}
				tmp++;
			}
			return tmp;
		}

		public void check() {
			assertThat(testFor()).isEqualTo(12);
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("break;")
				.containsOne("return 0;");
	}
}
