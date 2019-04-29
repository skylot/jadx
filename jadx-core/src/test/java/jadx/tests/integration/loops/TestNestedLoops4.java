package jadx.tests.integration.loops;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

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
	}

	@Test
	@NotYetImplemented
	public void test() {
		getClassNode(TestCls.class);
	}
}
