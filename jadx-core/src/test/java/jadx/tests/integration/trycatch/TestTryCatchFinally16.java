package jadx.tests.integration.trycatch;

import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally16 extends SmaliTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public void test() {
			try {
				TCls.doSomething();
			} catch (Exception e) {
				// do nothing
			} finally {
				TCls.doFinally();
			}
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
				.containsOne("finally")
				.containsOne("} catch")
				.contains("catch (Exception e)");
	}
}
