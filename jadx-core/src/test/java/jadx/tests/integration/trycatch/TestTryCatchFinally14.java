package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally14 extends IntegrationTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		private TCls t;

		public void test() {
			try {
				if (t != null) {
					t.doSomething();
				}
			} finally {
				if (t != null) {
					t.doFinally();
				}
			}
		}

		private static class TCls {
			public void doSomething() {
			}

			public void doFinally() {
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne(".doSomething();")
				.containsOne("} finally {")
				.containsOne(".doFinally();")
				.countString(2, "!= null) {");
	}
}
