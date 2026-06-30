package jadx.tests.integration.jbc;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestDup2x1 extends IntegrationTest {

	@SuppressWarnings({ "FieldCanBeLocal", "checkstyle:InnerAssignment", "unused" })
	public static class TestCls {
		private long value;

		public long setValue(long v) {
			return this.value = v;
		}
	}

	@TestWithProfiles(TestProfile.JAVA11)
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("this.value = v;");
	}
}
