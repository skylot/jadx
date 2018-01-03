package jadx.tests.integration.switches;

import org.junit.Test;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.IntegrationTest;

import static jadx.tests.api.utils.JadxMatchers.countString;
import static org.junit.Assert.assertThat;

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
		ClassNode cls = getClassNode(TestCls.class);
		String code = cls.getCode().toString();

//		assertThat(code, countString(3, "break;"));
		assertThat(code, countString(4, "return;"));

		// TODO: remove redundant break
		assertThat(code, countString(4, "break;"));
	}
}
