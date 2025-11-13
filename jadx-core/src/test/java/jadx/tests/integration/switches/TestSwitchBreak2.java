package jadx.tests.integration.switches;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchBreak2 extends IntegrationTest {

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public static class TestCls {
		private int value;

		public void test(int i, boolean b1, boolean b2) {
			setValue(-1);
			switch (i) {
				case 0:
					if (b1 && b2) {
						setValue(1);
						// no break here;
					} else {
						setValue(2);
						// no break here;
					}
					break;
				default:
					setValue(0);
					break;
			}
		}

		private void setValue(int value) {
			this.value = value;
		}

		public void check() {
			test(0, true, true);
			assertThat(value).isEqualTo(1);
			test(0, true, false);
			assertThat(value).isEqualTo(2);
			test(1, true, true);
			assertThat(value).isEqualTo(0);
		}
	}

	@TestWithProfiles({ TestProfile.JAVA11, TestProfile.D8_J11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break;");
	}
}
