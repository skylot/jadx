package jadx.tests.integration.trycatch;

import jadx.NotYetImplemented;
import jadx.core.dex.nodes.ClassNode;
import jadx.tests.api.SmaliTest;
import jadx.tests.api.extensions.profiles.TestProfile;
import jadx.tests.api.extensions.profiles.TestWithProfiles;

import static jadx.tests.api.utils.assertj.JadxAssertions.assertThat;

/**
 * A class which tests finally extraction for cases where the all handler does not rethrow an
 * exception.
 */
public class TestTryCatchFinally19 extends SmaliTest {

	@SuppressWarnings("unused")
	public static class TestCls {
		public Integer test() {
			Integer val;
			try {
				return TCls.doSomething();
			} catch (Throwable t) {
				return null;
			} finally {
				TCls.dispose();
			}
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

	@TestWithProfiles({ TestProfile.D8_J11 })
	@NotYetImplemented("Currently only processing finally blocks if the all handler throws.")
	public void testDXJ8() {
		disableCompilation();
		ClassNode node = getClassNode(TestCls.class);
		assertThat(node)
				.code()
				.containsOne("TCls.doSomething()")
				.containsOne("TCls.dispose()")
				.containsOne("} finally")
				.containsOne("catch (Throwable ")
				.containsOne("return null");
	}
}
