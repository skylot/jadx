package jadx.tests.integration.trycatch;

import jadx.tests.api.IntegrationTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestNestedTryCatch3 extends IntegrationTest {

	public static class TestCls {
		public I test() {
			try {
				try {
					return new A();
				} catch (Throwable e) {
					return new B();
				}
			} catch (Throwable e) {
				return new C();
			}
		}

		private interface I {
		}

		private static class A implements I {
		}

		private static class B implements I {
		}

		private static class C implements I {
		}
	}

	@TestWithProfiles({ TestProfile.JAVA8, TestProfile.DX_J8 })
	public void test() {
		assertThat(getClassNode(TestCls.class))
				.code()
				.containsOne("return new A();")
				.containsOne("return new B();")
				.containsOne("return new C();")
				.countString(2, "} catch (Throwable ");
	}
}
