package jadx.tests.integration.trycatch;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally17 extends SmaliTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public int test() {
			try {
				TCls.doSomething();
			} catch (UnsupportedOperationException e) {
				// do nothing
			} catch (NullPointerException e) {
				return 1;
			} finally {
				TCls.doFinally();
			}
			return 0;
		}

		private static class TCls {
			public static void doSomething() {
			}

			public static void doFinally() {
			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11, TestProfile.JAVA8 })
	public void test() {
		disableCompilation();
		ClassNode node = getClassNode(TestCls.class);
		assertThat(node)
				.code()
				.containsOne("TCls.doSomething()")
				.containsOne("TCls.doFinally()")
				.containsOne("} finally")
				.containsOne("catch (NullPointerException ")
				.containsOne("catch (UnsupportedOperationException ")
				.doesNotContain("catch (Throwable");
	}
}
