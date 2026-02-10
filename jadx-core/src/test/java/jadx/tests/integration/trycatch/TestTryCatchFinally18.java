package jadx.tests.integration.trycatch;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

public class TestTryCatchFinally18 extends SmaliTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public int test3() {
			int val;
			try {
				val = TCls.doSomething();
			} catch (UnsupportedOperationException e) {
				return -1;
			} catch (NullPointerException e) {
				val = 0;
			} finally {
				TCls.dispose();
			}
			val += 4;
			if (val < 10) {
				TCls.log("less than 10");
			}
			return val;
		}

		private static class TCls {
			public static int doSomething() {
				return 14;
			}

			public static void dispose() {
			}

			public static void log(String msg) {

			}
		}
	}

	@TestWithProfiles({ TestProfile.DX_J8, TestProfile.D8_J11 })
	public void test() {
		disableCompilation();
		ClassNode node = getClassNode(TestCls.class);
		assertThat(node)
				.code()
				.containsOne("TCls.doSomething()")
				.containsOne("TCls.dispose()")
				.containsOne("} finally")
				.containsOne("catch (NullPointerException ")
				.containsOne("catch (UnsupportedOperationException ")
				.doesNotContain("catch (Throwable");
	}

	@NotYetImplemented("To be investigated why J8 does not work")
	@TestWithProfiles({ TestProfile.JAVA8 })
	public void testJ8() {
		disableCompilation();
		ClassNode node = getClassNode(TestCls.class);
		assertThat(node)
				.code()
				.containsOne("TCls.doSomething()")
				.containsOne("TCls.dispose()")
				.containsOne("} finally")
				.containsOne("catch (NullPointerException ")
				.containsOne("catch (UnsupportedOperationException ")
				.doesNotContain("catch (Throwable");
	}
}
