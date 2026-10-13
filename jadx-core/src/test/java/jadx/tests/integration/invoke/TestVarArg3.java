package jadx.tests.integration.invoke;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestVarArg3 extends IntegrationTest {

	@SuppressWarnings("RedundantCast")
	public static class TestCls {
		public int count(Object... a) {
			return a == null ? -1 : a.length;
		}

		public int test1() {
			return count((Object) null);
		}

		public int test2(String[] strs) {
			return count((Object) strs);
		}

		public int test3(int[] ints) {
			return count(ints);
		}

		public void check() {
			assertThat(test1()).isEqualTo(1);
			assertThat(test2(new String[] { "a", "b" })).isEqualTo(1);
			assertThat(test3(new int[] { 1, 2 })).isEqualTo(1);
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA11 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.doesNotContain("count(null)")
				.doesNotContain("count(strs)")
				.containsOne("count(ints)");
	}
}
