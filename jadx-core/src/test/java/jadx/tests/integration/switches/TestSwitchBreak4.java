package jadx.tests.integration.switches;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestSwitchBreak4 extends IntegrationTest {

	@SuppressWarnings("SwitchStatementWithTooFewBranches")
	public static class TestCls {
		private int value;

		public void test(int i, boolean b1, boolean b2, boolean b3) {
			setValue(-1);
			switch (i) {
				case 0:
					if (b1 == b2) {
						setValue(1);
					} else if (b1 == b3) {
						setValue(2);
					} else {
						setValue(3);
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
			test(0, true, true, true);
			assertThat(value).isEqualTo(1);
			test(0, true, false, true);
			assertThat(value).isEqualTo(2);
			test(0, true, false, false);
			assertThat(value).isEqualTo(3);
		}
	}

	@TestWithProfiles({ TestProfile.JAVA11, TestProfile.D8_J11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.countString(2, "break;")
				.containsOne("} else if (")
				.containsOne("} else {");
	}
}
