package jadx.tests.integration.switches;

import org.junit.jupiter.api.Test;

import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

@SuppressWarnings("checkstyle:printstacktrace")
public class TestSwitchWithTryCatch extends IntegrationTest {
	public static class TestCls {
		void test(int a) {
			switch (a) {
				case 0:
					try {
						exc();
						return;
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					// no break;

				case 1:
					try {
						exc();
						return;
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;

				case 2:
					try {
						exc();
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}
					break;

				case 3:
					try {
						exc();
					} catch (Exception e) {
						e.printStackTrace();
					}
					break;
			}
			if (a == 10) {
				System.out.println(a);
			}
		}

		private void exc() throws Exception {
		}
	}

	@Test
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				// .countString(3, "break;")
				.countString(4, "return;")
				// TODO: remove redundant break
				.countString(4, "break;");
	}
}
