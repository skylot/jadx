package jadx.tests.integration.conditions;

import org.junit.jupiter.api.Test;

import jadx.NotYetImplemented;
import jadx.tests.api.IntegrationTest;

public class TestSwitchTryBreak2 extends IntegrationTest {

	public static class TestCls {

		public void test(int x) {
			switch (x) {
				case 0:
					return;
				case 1:
					String res;
					if ("android".equals(toString())) {
						res = "hello";
					} else {
						try {
							if (x == 5) {
								break;
							}
							res = "hi";
						} catch (Exception e) {
							break;
						}
					}
					System.out.println(res);
			}
			System.out.println("returning");
		}
	}

	@Test
	@NotYetImplemented
	public void test() {
		getClassNode(TestCls.class);
	}
}
